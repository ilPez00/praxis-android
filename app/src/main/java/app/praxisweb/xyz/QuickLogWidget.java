package app.praxisweb.xyz;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.json.JSONObject;

/**
 * One-tap tracker logging from the home screen.
 *
 * Four buttons, each firing a broadcast that enqueues a tracker log through
 * the app's offline write queue — so the tap "works" with no network at all:
 * the log lands in the pending-mutations table and syncs later. The toast is
 * the only feedback a widget can give.
 *
 * Buttons read their tracker type from the widget snapshot (`quicklog` array,
 * written by the server summary) with hardcoded water/movement/reading
 * defaults for anyone whose snapshot has not arrived yet. The fourth slot
 * always opens the app's trackers screen, because some logs need a number.
 */
public class QuickLogWidget extends AppWidgetProvider {

    public static final String ACTION_QUICK_LOG = "app.praxisweb.xyz.QUICK_LOG";
    public static final String EXTRA_TRACKER_TYPE = "tracker_type";

    private static final String[][] BUTTONS = {
            {"water", "\uD83D\uDCA7"},   // 💧
            {"movement", "\uD83C\uDFC3"}, // 🏃
            {"reading", "\uD83D\uDCD6"}, // 📖
    };

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            manager.updateAppWidget(id, build(context));
        }
    }

    static RemoteViews build(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quicklog);

        for (int i = 0; i < BUTTONS.length; i++) {
            Intent intent = new Intent(context, QuickLogWidget.class);
            intent.setAction(ACTION_QUICK_LOG);
            intent.putExtra(EXTRA_TRACKER_TYPE, BUTTONS[i][0]);
            views.setOnClickPendingIntent(buttonId(i), PendingIntentCompat(
                    context, intent, 100 + i));
            views.setTextViewText(textId(i), BUTTONS[i][1]);
        }

        views.setOnClickPendingIntent(R.id.widget_quicklog_more,
                PraxisWidgets.openApp(context, PraxisWidgets.ROUTE_TRACKERS, 140));
        return views;
    }

    private static android.app.PendingIntent PendingIntentCompat(Context context, Intent intent, int requestCode) {
        return android.app.PendingIntent.getBroadcast(
                context, requestCode, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
    }

    private static int buttonId(int i) {
        switch (i) {
            case 0: return R.id.widget_quicklog_b1;
            case 1: return R.id.widget_quicklog_b2;
            default: return R.id.widget_quicklog_b3;
        }
    }

    private static int textId(int i) {
        switch (i) {
            case 0: return R.id.widget_quicklog_t1;
            case 1: return R.id.widget_quicklog_t2;
            default: return R.id.widget_quicklog_t3;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (!ACTION_QUICK_LOG.equals(intent.getAction())) return;
        String type = intent.getStringExtra(EXTRA_TRACKER_TYPE);
        if (type == null || type.isEmpty()) return;

        // Fire-and-forget: the write goes through the app's durable offline
        // queue (Room) and syncs whenever the network next allows it.
        com.praxis.android.widget.WidgetQuickLog.enqueue(context.getApplicationContext(), type);
        Toast.makeText(context, "Logged " + type + " ✓", Toast.LENGTH_SHORT).show();
    }
}
