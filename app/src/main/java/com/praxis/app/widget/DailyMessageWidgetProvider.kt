package com.praxis.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.praxis.app.R

class DailyMessageWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_daily_message)
            val quote = WidgetDataStore.getAxiomQuote(context)
            if (quote.isNotEmpty()) {
                views.setTextViewText(R.id.txt_daily_quote, "\"$quote\"")
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
