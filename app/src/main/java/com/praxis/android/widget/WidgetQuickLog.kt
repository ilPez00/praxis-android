package com.praxis.android.widget

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Java-callable bridge for widget broadcasts to enqueue tracker logs.
 *
 * A BroadcastReceiver must return fast and has no coroutine scope of its own;
 * this launches one fire-and-forget write on IO. The queue itself is durable,
 * so if the process dies before the write completes the tap is lost — an
 * accepted cost for one-tap widgets (the alternative, goAsync + WorkManager
 * chaining, costs more than a single-row insert).
 */
object WidgetQuickLog {
    @JvmStatic
    fun enqueue(context: Context, trackerType: String) {
        val app = context.applicationContext as com.praxis.android.PraxisApp
        GlobalScope.launch(Dispatchers.IO) {
            runCatching { app.repository.queueTrackerLog(trackerType, mapOf("count" to 1)) }
        }
    }
}
