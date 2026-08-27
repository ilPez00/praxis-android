package app.praxisweb.xyz;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * The only place widget state is written or read.
 *
 * Two very different things live here and they are stored separately on
 * purpose:
 *
 *   - the **session** (Supabase access + refresh token), which is a credential
 *     and goes in {@link EncryptedSharedPreferences};
 *   - the **snapshot** (the last widget payload the server returned), which is
 *     ordinary user data and goes in plain preferences.
 *
 * Keeping the snapshot out of the encrypted store is deliberate: widget
 * rendering happens on the main thread during a broadcast, and the encrypted
 * store does keystore work on first open. Paying that to read a streak count is
 * how a widget update misses its frame budget. The credential is the only thing
 * worth the cost.
 */
public final class WidgetStore {

    private static final String TAG = "PraxisWidgetStore";

    private static final String SECURE_PREFS = "praxis_widget_secure";
    private static final String PLAIN_PREFS = "praxis_widget_data";

    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private static final String KEY_API_BASE = "api_base";
    private static final String KEY_SUPABASE_URL = "supabase_url";
    private static final String KEY_SUPABASE_ANON = "supabase_anon";

    private static final String KEY_SNAPSHOT = "snapshot_json";
    private static final String KEY_FETCHED_AT = "fetched_at";

    private final SharedPreferences secure;
    private final SharedPreferences plain;

    private WidgetStore(Context context) {
        this.plain = context.getSharedPreferences(PLAIN_PREFS, Context.MODE_PRIVATE);
        this.secure = openSecure(context);
    }

    public static WidgetStore get(Context context) {
        return new WidgetStore(context.getApplicationContext());
    }

    /**
     * Open the encrypted preferences, falling back to plain ones if the keystore
     * refuses.
     *
     * The fallback exists because a failure here is not recoverable by the user:
     * a corrupted keystore entry (which happens after some restore-from-backup
     * flows) throws on every open, and without a fallback the widget would be
     * permanently dead with no way to reset it short of reinstalling. Degrading
     * is the lesser harm, and it is logged rather than silent — but note the
     * token is then only as protected as the app sandbox.
     */
    private static SharedPreferences openSecure(Context context) {
        try {
            MasterKey key = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    SECURE_PREFS,
                    key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            Log.w(TAG, "Encrypted preferences unavailable, falling back to plain storage", e);
            return context.getSharedPreferences(SECURE_PREFS + "_fallback", Context.MODE_PRIVATE);
        }
    }

    // ---------------------------------------------------------------- session

    public void saveSession(String accessToken, String refreshToken, long expiresAtMs,
                            String apiBase, String supabaseUrl, String supabaseAnonKey) {
        secure.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_EXPIRES_AT, expiresAtMs)
                .putString(KEY_API_BASE, apiBase)
                .putString(KEY_SUPABASE_URL, supabaseUrl)
                .putString(KEY_SUPABASE_ANON, supabaseAnonKey)
                .apply();
    }

    /** Replace just the tokens after a refresh, leaving the endpoints alone. */
    public void updateTokens(String accessToken, String refreshToken, long expiresAtMs) {
        secure.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_EXPIRES_AT, expiresAtMs)
                .apply();
    }

    public String accessToken() {
        return secure.getString(KEY_ACCESS_TOKEN, null);
    }

    public String refreshToken() {
        return secure.getString(KEY_REFRESH_TOKEN, null);
    }

    public long expiresAt() {
        return secure.getLong(KEY_EXPIRES_AT, 0L);
    }

    public String apiBase() {
        return secure.getString(KEY_API_BASE, null);
    }

    public String supabaseUrl() {
        return secure.getString(KEY_SUPABASE_URL, null);
    }

    public String supabaseAnonKey() {
        return secure.getString(KEY_SUPABASE_ANON, null);
    }

    public boolean hasSession() {
        return accessToken() != null && apiBase() != null;
    }

    /**
     * Sign-out. Clears the credential *and* the snapshot together.
     *
     * These must go in the same call and never separately: a widget left showing
     * the previous account's streak and meals after someone signs out is a
     * privacy failure sitting on a home screen, visible without unlocking
     * anything.
     */
    public void clear() {
        secure.edit().clear().apply();
        plain.edit().clear().apply();
    }

    // --------------------------------------------------------------- snapshot

    public void saveSnapshot(String json) {
        plain.edit()
                .putString(KEY_SNAPSHOT, json)
                .putLong(KEY_FETCHED_AT, System.currentTimeMillis())
                .apply();
    }

    public String snapshot() {
        return plain.getString(KEY_SNAPSHOT, null);
    }

    public long fetchedAt() {
        return plain.getLong(KEY_FETCHED_AT, 0L);
    }
}
