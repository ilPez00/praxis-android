package com.praxis.android

import android.app.Application
import com.praxis.android.data.local.PraxisDatabase
import com.praxis.android.data.repository.PraxisRepository

class PraxisApp : Application() {
    lateinit var repository: PraxisRepository
        private set
    lateinit var database: PraxisDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        // The shell is always dark; force night mode so the WebView's
        // algorithmic darkening + prefers-color-scheme: dark apply to pages.
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES,
        )
        database = PraxisDatabase.getInstance(this)
        repository = PraxisRepository(this)
        // WorkManager's first getInstance() forces its synchronous startup init
        // on whichever thread calls it; keep that off the main thread so cold
        // start isn't blocked behind scheduler bookkeeping.
        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
            com.praxis.android.widget.WidgetUpdateManager.init(this)
            com.praxis.android.worker.SyncScheduler.schedule(this)
            app.praxisweb.xyz.WidgetRefreshWorker.schedule(this)
        }
    }
}
