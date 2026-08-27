package app.praxisweb.xyz;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONObject;

/**
 * Today's planner blocks.
 *
 * A collection widget, so the rows come from {@link TasksRemoteViewsService}
 * rather than being built here — RemoteViews cannot be handed a variable-length
 * list any other way.
 */
public class TasksWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            manager.updateAppWidget(id, build(context, id));
        }
        // The frame above redraws immediately; the rows do not. Without this the
        // widget shows a fresh header over yesterday's list.
        manager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_task_list);
    }

    static RemoteViews build(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_tasks);

        Intent serviceIntent = new Intent(context, TasksRemoteViewsService.class);
        // The adapter is cached per Intent, and Intent equality ignores extras.
        // Without a per-widget URI two instances of this widget would share one
        // adapter and render the same rows.
        serviceIntent.setData(android.net.Uri.parse("praxis://tasks/" + appWidgetId));
        views.setRemoteAdapter(R.id.widget_task_list, serviceIntent);

        JSONObject snapshot = PraxisWidgets.snapshot(context);
        boolean signedIn = WidgetStore.get(context).hasSession();
        int taskCount = PraxisWidgets.tasks(snapshot).length();

        if (!signedIn || taskCount == 0) {
            views.setViewVisibility(R.id.widget_task_list, View.GONE);
            views.setViewVisibility(R.id.widget_tasks_empty, View.VISIBLE);
            views.setTextViewText(R.id.widget_tasks_empty, context.getString(
                    signedIn ? R.string.widget_no_tasks : R.string.widget_signed_out));
        } else {
            views.setViewVisibility(R.id.widget_task_list, View.VISIBLE);
            views.setViewVisibility(R.id.widget_tasks_empty, View.GONE);
        }

        // Tapping the header opens the planner. Rows get their own intent via
        // the template below, filled in per row by the factory.
        views.setOnClickPendingIntent(R.id.widget_tasks_header,
                PraxisWidgets.openApp(context, PraxisWidgets.ROUTE_PLANNER, 3));
        views.setPendingIntentTemplate(R.id.widget_task_list,
                PraxisWidgets.openAppTemplate(context, 4));

        return views;
    }
}
