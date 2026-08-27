package app.praxisweb.xyz;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Supplies the rows for {@link TasksWidget}.
 *
 * Runs in the app's process but outside any Activity, on a thread the framework
 * owns. {@code onDataSetChanged} is allowed to block — and is the only place
 * that may — so the snapshot is read there once per refresh rather than per
 * row, which would re-read preferences a dozen times for one redraw.
 */
public class TasksRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new TasksFactory(getApplicationContext());
    }

    private static class TasksFactory implements RemoteViewsFactory {

        private final Context context;
        private JSONArray tasks = new JSONArray();

        /** Local-time formatter for the row's leading time column. */
        private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm", Locale.getDefault());
        private final SimpleDateFormat parser =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

        TasksFactory(Context context) {
            this.context = context;
            // Timestamps arrive in UTC; the user reads them in their own zone.
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override
        public void onCreate() {
            reload();
        }

        @Override
        public void onDataSetChanged() {
            reload();
        }

        private void reload() {
            tasks = PraxisWidgets.tasks(PraxisWidgets.snapshot(context));
        }

        @Override
        public void onDestroy() {
            tasks = new JSONArray();
        }

        @Override
        public int getCount() {
            return tasks.length();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_task_item);
            JSONObject task = tasks.optJSONObject(position);
            if (task == null) return row;

            String title = task.optString("title", "");
            boolean done = task.optBoolean("done", false);

            row.setTextViewText(R.id.widget_task_time, formatTime(task.optString("at", null)));
            row.setTextViewText(R.id.widget_task_title, title);
            // Completed work stays visible but recedes. Removing it would make a
            // finished day look like an empty one.
            row.setTextColor(R.id.widget_task_title, context.getColor(
                    done ? R.color.widget_text_secondary : R.color.widget_text_primary));

            // Fills in the template TasksWidget set. Every row opens the planner;
            // the per-row Intent exists so the tap lands somewhere at all.
            Intent fillIn = new Intent();
            fillIn.setData(Uri.parse(PraxisWidgets.ROUTE_PLANNER));
            row.setOnClickFillInIntent(R.id.widget_task_row, fillIn);

            return row;
        }

        /** "HH:mm", or empty when the block has no time rather than "00:00". */
        private String formatTime(String iso) {
            if (iso == null || iso.length() < 19) return "";
            try {
                Date parsed = parser.parse(iso.substring(0, 19));
                return parsed == null ? "" : clock.format(parsed);
            } catch (Exception e) {
                return "";
            }
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }
}
