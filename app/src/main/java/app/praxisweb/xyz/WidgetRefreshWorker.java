package app.praxisweb.xyz;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Refreshes the widget snapshot while the app is closed.
 *
 * A widget that only updates when you open the app is a screenshot. The whole
 * value is being right on a home screen you glance at without launching
 * anything, so the fetch has to happen in a process the user is not driving —
 * hence WorkManager rather than a timer, which the OS would kill with the
 * process.
 *
 * Note this is the one place in the codebase that talks to the API without a
 * browser. It sends no Origin header, which is why `src/app.ts` must keep
 * allowing origin-less requests.
 */
public class WidgetRefreshWorker extends Worker {

    private static final String TAG = "PraxisWidgetWorker";

    private static final String PERIODIC_WORK = "praxis-widget-refresh";
    private static final String ONESHOT_WORK = "praxis-widget-refresh-now";

    /**
     * Fifteen minutes is WorkManager's floor for periodic work; asking for less
     * silently gets you this anyway. In practice Doze stretches it further, and
     * that is fine — nothing here is time-critical, and pretending otherwise
     * would just spend battery to be wrong slightly less often.
     */
    private static final long REFRESH_MINUTES = 15;

    /** Refresh the token this far before it actually expires. */
    private static final long TOKEN_SKEW_MS = 60_000L;

    private static final int TIMEOUT_MS = 15_000;

    public WidgetRefreshWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /** Schedule the recurring refresh. Safe to call repeatedly. */
    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                WidgetRefreshWorker.class, REFRESH_MINUTES, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        // KEEP, not REPLACE: the app calls this on every launch, and REPLACE
        // would restart the period each time — an app opened often would then
        // never actually reach a scheduled run.
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    /** Fetch once, right now — used when the app has just signed in or synced. */
    public static void refreshNow(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(WidgetRefreshWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();
        WorkManager.getInstance(context).enqueue(request);
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK);
        WorkManager.getInstance(context).cancelAllWorkByTag(ONESHOT_WORK);
    }

    @NonNull
    @Override
    public Result doWork() {
        WidgetStore store = WidgetStore.get(getApplicationContext());

        // Signed out, or never signed in. Not a failure — there is simply
        // nothing to fetch, and retrying would burn battery forever.
        if (!store.hasSession()) {
            return Result.success();
        }

        try {
            if (System.currentTimeMillis() + TOKEN_SKEW_MS >= store.expiresAt()) {
                if (!refreshSession(store)) {
                    // Refresh token rejected — the user must sign in again. Keep
                    // the last snapshot on screen rather than blanking it; the
                    // app will re-sync when they next open it.
                    Log.w(TAG, "Session refresh failed; leaving last snapshot in place");
                    return Result.success();
                }
            }

            String body = fetchSummary(store);
            if (body == null) return Result.retry();

            store.saveSnapshot(body);
            PraxisWidgets.refreshAll(getApplicationContext());
            return Result.success();
        } catch (Exception e) {
            Log.w(TAG, "Widget refresh failed", e);
            return Result.retry();
        }
    }

    /** GET {apiBase}/widget/summary. Returns the body, or null to retry. */
    private String fetchSummary(WidgetStore store) throws Exception {
        URL url = new URL(store.apiBase() + "/widget/summary");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + store.accessToken());
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            int code = conn.getResponseCode();
            if (code == 200) return readAll(conn.getInputStream());

            // A 401 here means the token died earlier than its stated expiry.
            // One refresh-and-retry, then give up until the next scheduled run
            // rather than looping against a server that is saying no.
            if (code == 401 && refreshSession(store)) {
                return fetchSummary(store);
            }

            Log.w(TAG, "Widget summary returned HTTP " + code);
            return null;
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Exchange the refresh token for a new session against Supabase directly.
     *
     * The widget cannot ask the WebView to do this — there may be no WebView
     * alive — so it speaks the same auth endpoint the client does.
     */
    private boolean refreshSession(WidgetStore store) throws Exception {
        String refreshToken = store.refreshToken();
        String supabaseUrl = store.supabaseUrl();
        String anonKey = store.supabaseAnonKey();
        if (refreshToken == null || supabaseUrl == null || anonKey == null) return false;

        URL url = new URL(supabaseUrl + "/auth/v1/token?grant_type=refresh_token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", anonKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoOutput(true);

            JSONObject payload = new JSONObject().put("refresh_token", refreshToken);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) return false;

            JSONObject json = new JSONObject(readAll(conn.getInputStream()));
            String access = json.optString("access_token", null);
            String refresh = json.optString("refresh_token", refreshToken);
            long expiresIn = json.optLong("expires_in", 3600L);
            if (access == null) return false;

            store.updateTokens(access, refresh, System.currentTimeMillis() + expiresIn * 1000L);
            return true;
        } finally {
            conn.disconnect();
        }
    }

    private static String readAll(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}
