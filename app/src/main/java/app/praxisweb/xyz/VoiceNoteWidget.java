package app.praxisweb.xyz;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

/**
 * One tap straight into a voice note.
 *
 * Like CaptureWidget it carries no data — it works signed-out, offline and
 * before any refresh has run. The mic is the point: speaking a thought into
 * the phone must not require finding the app, launching it, navigating
 * anywhere. The deep link lands directly on the capture screen, which starts
 * recording as soon as the microphone permission exists.
 */
public class VoiceNoteWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            manager.updateAppWidget(id, build(context));
        }
    }

    static RemoteViews build(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_voice);
        views.setOnClickPendingIntent(R.id.widget_voice_root,
                PraxisWidgets.openApp(context, PraxisWidgets.ROUTE_VOICE_NOTE, 150));
        return views;
    }
}
