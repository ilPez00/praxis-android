package app.praxisweb.xyz;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import org.json.JSONObject;

/**
 * Streak count and whether today's check-in is done.
 *
 * The check-in state is the reason this widget earns its place: the streak
 * number alone is a trophy, but "not yet today" at eight in the evening is the
 * nudge. Tapping anywhere opens straight into check-in rather than the app's
 * home, so acting on that nudge is one tap and not four.
 */
public class StreakWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            manager.updateAppWidget(id, build(context));
        }
    }

    static RemoteViews build(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_streak);
        JSONObject snapshot = PraxisWidgets.snapshot(context);

        boolean signedIn = WidgetStore.get(context).hasSession();
        if (!signedIn) {
            // Distinguish "signed out" from "streak of zero". They look
            // identical otherwise, and one of them is a thing the user can fix.
            views.setTextViewText(R.id.widget_streak_value, "–");
            views.setTextViewText(R.id.widget_streak_status,
                    context.getString(R.string.widget_signed_out));
        } else {
            int streak = PraxisWidgets.streak(snapshot);
            boolean checkedIn = PraxisWidgets.checkedInToday(snapshot);

            views.setTextViewText(R.id.widget_streak_value, String.valueOf(streak));
            views.setTextViewText(R.id.widget_streak_status, context.getString(
                    checkedIn ? R.string.widget_checked_in : R.string.widget_not_checked_in));
            views.setTextColor(R.id.widget_streak_status, context.getColor(
                    checkedIn ? R.color.widget_positive : R.color.widget_text_secondary));
        }

        views.setOnClickPendingIntent(R.id.widget_streak_root,
                PraxisWidgets.openApp(context, PraxisWidgets.ROUTE_CHECKIN, 1));
        return views;
    }
}
