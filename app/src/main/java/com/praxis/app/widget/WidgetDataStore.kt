package com.praxis.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import com.praxis.app.data.model.User

/**
 * SharedPreferences-backed cache for widget data.
 *
 * Widgets run as BroadcastReceivers and can't use coroutines or Room directly.
 * The ViewModel writes here after every successful API load; widgets read synchronously.
 */
object WidgetDataStore {
    private const val PREFS = "praxis_widget_cache"
    private const val KEY_STREAK = "streak"
    private const val KEY_POINTS = "praxis_points"
    private const val KEY_GOAL_NAME = "top_goal_name"
    private const val KEY_GOAL_EMOJI = "top_goal_emoji"
    private const val KEY_GOAL_PROGRESS = "top_goal_progress"
    private const val KEY_GOAL_ID = "top_goal_id"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_AXIOM_QUOTE = "axiom_quote"
    private const val KEY_AXIOM_LAST_MESSAGE = "axiom_last_message"
    private const val KEY_TRACKER_COUNT = "tracker_count"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(context: Context, user: User, quote: String? = null, trackerCount: Int = 0, lastAxiom: String? = null) {
        val topGoal = user.goalTree.firstOrNull()
        prefs(context).edit().apply {
            putInt(KEY_STREAK, user.currentStreak)
            putInt(KEY_POINTS, user.praxisPoints)
            putString(KEY_GOAL_NAME, topGoal?.name ?: "")
            putString(KEY_GOAL_EMOJI, topGoal?.emoji ?: "")
            putInt(KEY_GOAL_PROGRESS, topGoal?.progress ?: 0)
            putString(KEY_GOAL_ID, topGoal?.id ?: "")
            putString(KEY_USER_ID, user.id)
            if (quote != null) putString(KEY_AXIOM_QUOTE, quote)
            if (lastAxiom != null) putString(KEY_AXIOM_LAST_MESSAGE, lastAxiom)
            putInt(KEY_TRACKER_COUNT, trackerCount)
            apply()
        }

        // Tell Android to redraw all Praxis widgets immediately
        refreshAllWidgets(context)
    }

    fun getStreak(context: Context): Int = prefs(context).getInt(KEY_STREAK, 0)
    fun getPraxisPoints(context: Context): Int = prefs(context).getInt(KEY_POINTS, 0)
    fun getGoalName(context: Context): String = prefs(context).getString(KEY_GOAL_NAME, "") ?: ""
    fun getGoalEmoji(context: Context): String = prefs(context).getString(KEY_GOAL_EMOJI, "") ?: ""
    fun getGoalProgress(context: Context): Int = prefs(context).getInt(KEY_GOAL_PROGRESS, 0)
    fun getGoalId(context: Context): String = prefs(context).getString(KEY_GOAL_ID, "") ?: ""
    fun getUserId(context: Context): String = prefs(context).getString(KEY_USER_ID, "") ?: ""
    fun getAxiomQuote(context: Context): String = prefs(context).getString(KEY_AXIOM_QUOTE, "") ?: ""
    fun getLastAxiomMessage(context: Context): String = prefs(context).getString(KEY_AXIOM_LAST_MESSAGE, "") ?: ""
    fun getTrackerCount(context: Context): Int = prefs(context).getInt(KEY_TRACKER_COUNT, 0)

    fun hasData(context: Context): Boolean = getUserId(context).isNotEmpty()

    private fun refreshAllWidgets(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        listOf(
            StatsWidgetProvider::class.java, 
            ProgressWidgetProvider::class.java,
            AxiomWidgetProvider::class.java,
            DailyMessageWidgetProvider::class.java,
            TrackerWidgetProvider::class.java
        ).forEach { cls ->
            val ids = mgr.getAppWidgetIds(ComponentName(context, cls))
            if (ids.isNotEmpty()) {
                // For simple widgets, we just update the whole view
                mgr.updateAppWidget(ids, null) 
            }
        }
    }
}
