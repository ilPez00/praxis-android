package com.praxis.android.widget

import android.content.Context

/**
 * The single place the app asks its home-screen widgets to redraw.
 *
 * The four widgets (streak / tasks / capture / charts) live in the
 * `app.praxisweb.xyz` package and draw from the widget snapshot the refresh
 * worker fetches from `/api/widget/summary`; this object just fans an update
 * out to all of them, and schedules an immediate fetch so the data under the
 * redraw is fresh.
 */
object WidgetUpdateManager {
    private var applicationContext: Context? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    fun requestWidgetUpdate() {
        val ctx = applicationContext ?: return
        app.praxisweb.xyz.PraxisWidgets.refreshAll(ctx)
        app.praxisweb.xyz.WidgetRefreshWorker.refreshNow(ctx)
    }
}
