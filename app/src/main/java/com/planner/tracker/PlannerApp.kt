package com.planner.tracker

import android.app.Application
import com.planner.tracker.data.AppDatabase

class PlannerApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
