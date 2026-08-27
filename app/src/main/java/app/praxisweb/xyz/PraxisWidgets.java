package app.praxisweb.xyz;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import com.praxis.android.MainActivity;

/**
 * Shared helpers for the home-screen widgets: telling them all to redraw, and
 * reading the snapshot they draw from.
 *
 * The snapshot is parsed here rather than in each provider so that the four
 * widgets cannot disagree about what a field means — and so that a payload
 * arriving from a newer server than the installed app degrades to zeros instead
 * of throwing inside a broadcast receiver, which the user would see as the
 * widget vanishing and replacing itself with "Problem loading widget".
 */
public final class PraxisWidgets {

    private PraxisWidgets() {}

    /** Deep links the widgets open. Handled by {@link MainActivity}. */
    public static final String ROUTE_CHECKIN = "praxis://checkin";
    public static final String ROUTE_NOTEBOOK_CAPTURE = "praxis://notebook?capture=1";
    public static final String ROUTE_PLANNER = "praxis://planner";
    public static final String ROUTE_TRACKERS = "praxis://trackers";
    public static final String ROUTE_VOICE_NOTE = "praxis://capture-audio";
    public static final String ROUTE_CONTACTS = "praxis://contacts";

    private static final Class<?>[] PROVIDERS = {
            StreakWidget.class,
            TasksWidget.class,
            CaptureWidget.class,
            ChartsWidget.class,
            QuickLogWidget.class,
            VoiceNoteWidget.class,
    };

    /**
     * Ask every Praxis widget to redraw.
     *
     * Called after a refresh lands. Collection widgets need the extra
     * {@code notifyAppWidgetViewDataChanged} call — an update broadcast alone
     * redraws the frame but leaves the list showing its previous rows.
     */
    public static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);

        for (Class<?> provider : PROVIDERS) {
            ComponentName component = new ComponentName(context, provider);
            int[] ids = manager.getAppWidgetIds(component);
            if (ids.length == 0) continue;

            Intent intent = new Intent(context, provider);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            context.sendBroadcast(intent);

            if (provider == TasksWidget.class) {
                manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_task_list);
            }
        }
    }

    /** The last snapshot, or null when nothing has been fetched yet. */
    public static JSONObject snapshot(Context context) {
        String raw = WidgetStore.get(context).snapshot();
        if (raw == null) return null;
        try {
            return new JSONObject(raw);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * A tap target that opens the app at {@code route}.
     *
     * {@code FLAG_IMMUTABLE} is required from API 31 (this app's minSdk) and is
     * the right choice anyway: a mutable PendingIntent handed to the launcher is
     * an intent any other app holding it could rewrite.
     */
    public static PendingIntent openApp(Context context, String route, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(route));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * The template a collection widget's rows fill in.
     *
     * This one has to be {@code FLAG_MUTABLE}: a row supplies its own
     * fill-in Intent, and from Android 12 an immutable template rejects that
     * outright, so every row tap would do nothing. Mutable is acceptable
     * specifically because the Intent names its component explicitly
     * ({@link MainActivity}) — the launcher can add a route to it, but cannot
     * redirect it at another app or grant itself a permission through it. The
     * rows only ever fill in a route string.
     */
    public static PendingIntent openAppTemplate(Context context, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    // ------------------------------------------------------------- accessors
    //
    // Every one of these is total: a missing key is a zero or an empty list, not
    // an exception. Widgets render inside a broadcast with no UI to report an
    // error to.

    public static int streak(JSONObject snapshot) {
        if (snapshot == null) return 0;
        return snapshot.optJSONObject("streak") == null
                ? 0
                : snapshot.optJSONObject("streak").optInt("current", 0);
    }

    public static boolean checkedInToday(JSONObject snapshot) {
        if (snapshot == null) return false;
        JSONObject streak = snapshot.optJSONObject("streak");
        return streak != null && streak.optBoolean("checkedInToday", false);
    }

    public static JSONArray tasks(JSONObject snapshot) {
        if (snapshot == null) return new JSONArray();
        JSONArray tasks = snapshot.optJSONArray("tasks");
        return tasks == null ? new JSONArray() : tasks;
    }

    public static JSONObject macrosToday(JSONObject snapshot) {
        if (snapshot == null) return new JSONObject();
        JSONObject nutrition = snapshot.optJSONObject("nutrition");
        if (nutrition == null) return new JSONObject();
        JSONObject today = nutrition.optJSONObject("today");
        return today == null ? new JSONObject() : today;
    }

    /** Series values only, oldest first — what a sparkline needs. */
    public static double[] series(JSONObject snapshot, String group, String key) {
        if (snapshot == null) return new double[0];
        JSONObject parent = snapshot.optJSONObject(group);
        if (parent == null) return new double[0];
        JSONArray arr = parent.optJSONArray(key);
        if (arr == null) return new double[0];

        double[] out = new double[arr.length()];
        for (int i = 0; i < arr.length(); i++) {
            JSONObject point = arr.optJSONObject(i);
            out[i] = point == null ? 0d : point.optDouble("value", 0d);
        }
        return out;
    }
}
