package com.praxis.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.praxis.app.MainActivity
import com.praxis.app.R

class AxiomWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_axiom)
            val lastMessage = WidgetDataStore.getLastAxiomMessage(context)
            if (lastMessage.isNotEmpty()) {
                views.setTextViewText(R.id.txt_axiom_message, lastMessage)
            }

            val appIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.btn_ask_axiom, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
