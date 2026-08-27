package app.praxisweb.xyz;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.util.Locale;

/**
 * Quick logs: calories and macros today, lift volume over the week.
 *
 * The chart is a {@link Bitmap} drawn here and set on an ImageView, because
 * RemoteViews cannot host a custom View — no chart library, no Canvas view, no
 * Compose. Everything visual on this widget is either a TextView or these
 * pixels.
 *
 * Two series share the x-axis (the same seven days) but not the y-axis: they are
 * scaled independently, since kilocalories and kilograms have no meaningful
 * common scale. That is a real limitation of the display and the reason each
 * series carries its own coloured total in the header rather than relying on the
 * bars to convey magnitude.
 */
public class ChartsWidget extends AppWidgetProvider {

    /**
     * Ceiling on the rendered bitmap.
     *
     * RemoteViews cross a Binder transaction with a hard size limit, and a
     * bitmap that exceeds it does not degrade — the update is dropped and the
     * widget silently keeps showing the previous frame. Capping is what keeps a
     * large or oddly-scaled home screen from producing a widget that mysteriously
     * stops updating.
     */
    private static final int MAX_PX = 720;

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            manager.updateAppWidget(id, build(context, manager, id));
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                          int appWidgetId, Bundle newOptions) {
        // Resized: the bitmap was drawn for the old dimensions and would be
        // stretched by scaleType until the next refresh.
        manager.updateAppWidget(appWidgetId, build(context, manager, appWidgetId));
    }

    static RemoteViews build(Context context, AppWidgetManager manager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_charts);
        JSONObject snapshot = PraxisWidgets.snapshot(context);

        if (!WidgetStore.get(context).hasSession()) {
            views.setTextViewText(R.id.widget_charts_calories, "–");
            views.setTextViewText(R.id.widget_charts_volume, "");
            views.setTextViewText(R.id.widget_charts_macros,
                    context.getString(R.string.widget_signed_out));
            views.setOnClickPendingIntent(R.id.widget_charts_root,
                    PraxisWidgets.openApp(context, PraxisWidgets.ROUTE_TRACKERS, 5));
            return views;
        }

        JSONObject macros = PraxisWidgets.macrosToday(snapshot);
        double calories = macros.optDouble("calories", 0d);
        double protein = macros.optDouble("protein_g", 0d);
        double carbs = macros.optDouble("carbs_g", 0d);
        double fat = macros.optDouble("fat_g", 0d);

        double[] calorieSeries = PraxisWidgets.series(snapshot, "nutrition", "calorieSeries");
        double[] volumeSeries = PraxisWidgets.series(snapshot, "lifts", "volumeSeries");
        double volumeTotal = 0d;
        for (double v : volumeSeries) volumeTotal += v;

        views.setTextViewText(R.id.widget_charts_calories,
                String.format(Locale.getDefault(), "%,.0f %s",
                        calories, context.getString(R.string.widget_calories)));
        views.setTextViewText(R.id.widget_charts_volume,
                String.format(Locale.getDefault(), "%,.0f %s",
                        volumeTotal, context.getString(R.string.widget_volume)));
        views.setTextViewText(R.id.widget_charts_macros,
                String.format(Locale.getDefault(), "P %.0fg · C %.0fg · F %.0fg",
                        protein, carbs, fat));

        Bitmap chart = drawChart(context, manager, appWidgetId, calorieSeries, volumeSeries);
        if (chart != null) {
            views.setImageViewBitmap(R.id.widget_charts_image, chart);
        }

        views.setOnClickPendingIntent(R.id.widget_charts_root,
                PraxisWidgets.openApp(context, PraxisWidgets.ROUTE_TRACKERS, 5));
        return views;
    }

    /** Paired bars per day: calories below, lift volume above, scaled separately. */
    private static Bitmap drawChart(Context context, AppWidgetManager manager, int appWidgetId,
                                    double[] calories, double[] volume) {
        int days = Math.max(calories.length, volume.length);
        if (days == 0) return null;

        int[] size = pixelSize(context, manager, appWidgetId);
        int width = size[0];
        int height = size[1];
        if (width <= 0 || height <= 0) return null;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        float slot = (float) width / days;
        float barWidth = Math.max(2f, slot * 0.32f);
        float gap = Math.max(1f, slot * 0.08f);
        float radius = barWidth / 2f;

        // Each series occupies half the height and is normalised to its own peak,
        // so a rest day reads as a gap rather than as a shorter bar on a scale
        // dominated by the other metric.
        float half = height / 2f;
        double calMax = max(calories);
        double volMax = max(volume);

        for (int i = 0; i < days; i++) {
            float centre = slot * i + slot / 2f;

            double cal = i < calories.length ? calories[i] : 0d;
            if (cal > 0 && calMax > 0) {
                float barHeight = (float) (cal / calMax) * (half - 4f);
                paint.setColor(context.getColor(R.color.widget_calories));
                canvas.drawRoundRect(new RectF(
                        centre - barWidth - gap / 2f, height - barHeight,
                        centre - gap / 2f, height), radius, radius, paint);
            }

            double vol = i < volume.length ? volume[i] : 0d;
            if (vol > 0 && volMax > 0) {
                float barHeight = (float) (vol / volMax) * (half - 4f);
                paint.setColor(context.getColor(R.color.widget_volume));
                canvas.drawRoundRect(new RectF(
                        centre + gap / 2f, half - barHeight,
                        centre + barWidth + gap / 2f, half), radius, radius, paint);
            }
        }

        return bitmap;
    }

    private static double max(double[] values) {
        double out = 0d;
        for (double v : values) if (v > out) out = v;
        return out;
    }

    /**
     * The widget's current size in pixels, from the launcher's own reported dp.
     *
     * A widget is never told its size directly; these options are the only
     * signal, and they can be absent (0) right after placement, so both fall
     * back to the declared minimum from widget_charts_info.xml.
     */
    private static int[] pixelSize(Context context, AppWidgetManager manager, int appWidgetId) {
        Bundle options = manager.getAppWidgetOptions(appWidgetId);
        int widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
        int heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110);
        if (widthDp <= 0) widthDp = 250;
        if (heightDp <= 0) heightDp = 110;

        // The chart occupies the lower portion of the card, under two text rows.
        int chartHeightDp = Math.max(40, heightDp - 46);

        return new int[]{
                Math.min(MAX_PX, dpToPx(context, widthDp)),
                Math.min(MAX_PX, dpToPx(context, chartHeightDp)),
        };
    }

    private static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }
}
