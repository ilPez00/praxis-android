package com.praxis.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.praxis.app.R
import com.praxis.app.WebAppActivity
import com.praxis.app.integrations.IntegrationManager
import com.praxis.app.integrations.IntegrationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TrackerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_tracker)
            
            // Get tracker count from data store
            val count = WidgetDataStore.getTrackerCount(context)
            views.setTextViewText(R.id.txt_tracker_count, count.toString())
            
            // Update integration status indicators
            updateIntegrationIndicators(context, views)

            // Click opens the webapp
            val intent = Intent(context, WebAppActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.tracker_widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        /**
         * Update visual indicators for connected integrations
         */
        private fun updateIntegrationIndicators(context: Context, views: RemoteViews) {
            // Show which integrations are active
            val healthConnectConnected = IntegrationManager.isConnected(IntegrationType.HEALTH_CONNECT)
            val stravaConnected = IntegrationManager.isConnected(IntegrationType.STRAVA)
            val fitbitConnected = IntegrationManager.isConnected(IntegrationType.FITBIT)
            val calendarConnected = IntegrationManager.isConnected(IntegrationType.GOOGLE_CALENDAR)
            val yazioConnected = IntegrationManager.isConnected(IntegrationType.YAZIO)
            
            // Update integration status text
            val activeIntegrations = mutableListOf<String>()
            if (healthConnectConnected) activeIntegrations.add("Health")
            if (stravaConnected) activeIntegrations.add("Strava")
            if (fitbitConnected) activeIntegrations.add("Fitbit")
            if (calendarConnected) activeIntegrations.add("Calendar")
            if (yazioConnected) activeIntegrations.add("Yazio")
            
            val statusText = if (activeIntegrations.isNotEmpty()) {
                "Auto-sync: ${activeIntegrations.joinToString(", ")}"
            } else {
                "No integrations connected"
            }
            
            views.setTextViewText(R.id.txt_integration_status, statusText)
        }
        
        /**
         * Update widget with live data (called from sync)
         */
        fun updateWithLiveData(context: Context, trackerCount: Int) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, TrackerWidgetProvider::class.java)
            )
            
            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_tracker)
                views.setTextViewText(R.id.txt_tracker_count, trackerCount.toString())
                updateIntegrationIndicators(context, views)
                
                val intent = Intent(context, WebAppActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(R.id.tracker_widget_container, pendingIntent)
                
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
