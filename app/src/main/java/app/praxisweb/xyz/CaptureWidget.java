package app.praxisweb.xyz;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

/**
 * One tap into the notebook's quick-note composer.
 *
 * The only widget with no data behind it at all — it never reads the snapshot,
 * so it works signed-out, offline, and before the first refresh has ever run.
 * That is deliberate: capture is the thing you most want to be instant and
 * least want to depend on a network round trip. The note itself is queued by
 * the app's existing offline write path once it opens.
 */
public class CaptureWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            manager.updateAppWidget(id, build(context));
        }
    }

    static RemoteViews build(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_capture);
        views.setOnClickPendingIntent(R.id.widget_capture_root,
                PraxisWidgets.openApp(context, PraxisWidgets.ROUTE_NOTEBOOK_CAPTURE, 2));
        return views;
    }
}
