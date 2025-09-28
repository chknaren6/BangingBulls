package com.nc.bangingbulls.Home.Stocks

import android.app.Application
import androidx.work.*
import java.time.Duration
import java.util.concurrent.TimeUnit

class AppWithEconomy : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleEconomy()
    }

    private fun scheduleEconomy() {
        // Quarter-hour periodic tick
        val tick = PeriodicWorkRequestBuilder<EconomyTickWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "economy_tick",
            ExistingPeriodicWorkPolicy.UPDATE,
            tick
        )

        // Nightly archive once a day. Start delay so it tends to run near local 23:59.
        val archive = PeriodicWorkRequestBuilder<NightlyArchiveWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.ofMinutes(1)) // small offset; optional
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "economy_archive",
            ExistingPeriodicWorkPolicy.UPDATE,
            archive
        )
    }
}


