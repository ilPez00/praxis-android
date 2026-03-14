package com.praxis.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.praxis.app.MainActivity
import com.praxis.app.R

/**
 * Widget for displaying user statistics (streak, points).
 * Data is read from WidgetDataStore (SharedPreferences), which is populated
 * by PraxisViewModel after each successful API load.
 */
class StatsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_stats)

            views.setTextViewText(R.id.txt_streak_value, WidgetDataStore.getStreak(context).toString())
            views.setTextViewText(R.id.txt_points_value, WidgetDataStore.getPraxisPoints(context).toString())

            val appIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.stats_widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
