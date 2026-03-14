package com.praxis.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.praxis.app.MainActivity
import com.praxis.app.R
import com.praxis.app.data.ApiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Widget for tracking goal progress from the home screen.
 * Data is read from WidgetDataStore (SharedPreferences), populated by PraxisViewModel.
 * The +5% button updates the local cache optimistically and syncs to the API in the background.
 */
class ProgressWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_INCREMENT_PROGRESS) return

        val nodeId = WidgetDataStore.getGoalId(context).takeIf { it.isNotEmpty() } ?: return
        val userId = WidgetDataStore.getUserId(context).takeIf { it.isNotEmpty() } ?: return

        // Optimistic local update
        val newProgress = (WidgetDataStore.getGoalProgress(context) + 5).coerceAtMost(100)
        context.getSharedPreferences("praxis_widget_cache", Context.MODE_PRIVATE)
            .edit().putInt("top_goal_progress", newProgress).apply()

        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(intent.component)
        onUpdate(context, mgr, ids)

        // Sync to API
        CoroutineScope(Dispatchers.IO).launch {
            ApiRepository().updateNodeProgress(userId, nodeId, newProgress)
        }
    }

    companion object {
        const val ACTION_INCREMENT_PROGRESS = "com.praxis.app.ACTION_INCREMENT_PROGRESS"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_progress)

            if (!WidgetDataStore.hasData(context)) {
                views.setTextViewText(R.id.widget_title, "No goals active")
                views.setTextViewText(R.id.widget_goal_name, "Open Praxis to add goals")
            } else {
                val goalName = WidgetDataStore.getGoalName(context)
                val goalEmoji = WidgetDataStore.getGoalEmoji(context)
                val progress = WidgetDataStore.getGoalProgress(context)

                views.setTextViewText(R.id.widget_title, "Goal Progress")
                views.setTextViewText(
                    R.id.widget_goal_name,
                    if (goalEmoji.isNotEmpty()) "$goalEmoji $goalName" else goalName,
                )
                views.setTextViewText(R.id.widget_progress_text, "$progress%")
                views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)

                val incrementIntent = Intent(context, ProgressWidgetProvider::class.java).apply {
                    action = ACTION_INCREMENT_PROGRESS
                }
                val incrementPending = PendingIntent.getBroadcast(
                    context, 0, incrementIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.btn_increment, incrementPending)
            }

            val appIntent = Intent(context, MainActivity::class.java)
            val appPending = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, appPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
