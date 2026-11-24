package com.planner.tracker

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.planner.tracker.data.AppDatabase
import com.planner.tracker.workers.InactivityCheckWorker
import java.util.concurrent.TimeUnit

class PlannerApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        val inactivityRequest = PeriodicWorkRequestBuilder<InactivityCheckWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "inactivity_check",
            ExistingPeriodicWorkPolicy.KEEP,
            inactivityRequest
        )
    }
}
