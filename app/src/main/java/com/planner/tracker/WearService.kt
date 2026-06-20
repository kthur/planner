package com.planner.tracker

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.planner.tracker.data.AppDatabase
import com.planner.tracker.data.CalendarSyncManager
import com.planner.tracker.data.Entry
import com.planner.tracker.data.Repository
import com.planner.tracker.data.WearDataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class WearService : WearableListenerService() {
    companion object {
        private const val TAG = "WearService"
    }
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "onMessageReceived: path=${messageEvent.path}")

        if (messageEvent.path == "/tracking/command") {
            val payload = String(messageEvent.data)
            Log.d(TAG, "Command payload received: $payload")
            try {
                val json = JSONObject(payload)
                val command = json.getString("command")
                handleCommand(command, json)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse or execute command", e)
            }
        }
    }

    private fun handleCommand(command: String, json: JSONObject) {
        val ctx = applicationContext
        val prefs = ctx.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)

        when (command) {
            "START" -> {
                val categoriesStr = json.optString("categories", "")
                val categoryDisplays = json.optString("category_displays", "")
                val note = json.optString("note", "")
                val timerMinutes = json.optString("timer_minutes", "")

                val categories = if (categoriesStr.isEmpty()) emptySet() else categoriesStr.split(",").toSet()
                val timerSec = (timerMinutes.toIntOrNull() ?: 0) * 60
                val nowElapsed = SystemClock.elapsedRealtime()

                // 1. Save state in SharedPreferences
                prefs.edit().apply {
                    putBoolean("is_tracking", true)
                    putLong("start_elapsed", nowElapsed)
                    putInt("target_sec", timerSec)
                    putString("timer_minutes", timerMinutes)
                    putString("note", note)
                    putString("categories", categoriesStr)
                    putString("category_displays", categoryDisplays)
                }.apply()

                // 2. Start Foreground Service
                val serviceIntent = Intent(ctx, TrackerService::class.java).apply {
                    action = TrackerService.ACTION_START
                    putExtra(TrackerService.EXTRA_NOTE, note)
                    putExtra(TrackerService.EXTRA_CATEGORIES, categoriesStr)
                    putExtra(TrackerService.EXTRA_CATEGORY_DISPLAYS, categoryDisplays)
                    putExtra(TrackerService.EXTRA_TIMER_MINUTES, timerMinutes)
                }
                try {
                    ctx.startForegroundService(serviceIntent)
                } catch (e: Exception) {
                    ctx.startService(serviceIntent)
                }

                // 3. Sync state back to Wear OS
                WearDataManager.syncTrackingState(
                    context = ctx,
                    isTracking = true,
                    startElapsed = nowElapsed,
                    targetSec = timerSec,
                    note = note,
                    categories = categories,
                    categoryDisplays = categoryDisplays,
                    photoUri = null
                )
            }
            "STOP" -> {
                serviceScope.launch {
                    val isTracking = prefs.getBoolean("is_tracking", false)
                    if (!isTracking) {
                        Log.w(TAG, "STOP received but not currently tracking.")
                        return@launch
                    }

                    val startElapsed = prefs.getLong("start_elapsed", 0L)
                    val targetSec = prefs.getInt("target_sec", 0)
                    val categoriesStr = prefs.getString("categories", "") ?: ""
                    val note = prefs.getString("note", "") ?: ""

                    // Stop the service
                    val stopIntent = Intent(ctx, TrackerService::class.java).apply {
                        action = TrackerService.ACTION_STOP
                    }
                    ctx.startService(stopIntent)

                    val nowTime = System.currentTimeMillis()
                    val actualSeconds = (SystemClock.elapsedRealtime() - startElapsed) / 1000
                    val savedSeconds = if (targetSec > 0) minOf(actualSeconds, targetSec.toLong()) else actualSeconds
                    val minutes = (savedSeconds / 60).toInt()

                    val startWallClock = nowTime - (SystemClock.elapsedRealtime() - startElapsed)
                    val endTimeValue = if (targetSec > 0 && actualSeconds > targetSec) {
                        startWallClock + targetSec * 1000L
                    } else {
                        nowTime
                    }

                    // Save to database
                    val categories = if (categoriesStr.isEmpty()) emptySet() else categoriesStr.split(",").toSet()
                    if (minutes > 0 && categories.isNotEmpty()) {
                        val db = AppDatabase.getInstance(ctx)
                        val repository = Repository(db.entryDao(), db.goalDao(), db.categoryDao())
                        val dayRange = Repository.getDayRange(startWallClock)

                        categories.forEach { category ->
                            val catInfo = repository.getCategoryByName(category)
                            val displayName = catInfo?.displayName ?: category

                            val tempEntry = Entry(
                                date = dayRange.first,
                                category = category,
                                minutes = minutes,
                                note = note,
                                startTime = startWallClock,
                                endTime = endTimeValue
                            )
                            val eventId = CalendarSyncManager.addEvent(ctx, tempEntry, displayName)
                            repository.insertEntry(tempEntry.copy(calendarEventId = eventId))
                        }
                    }

                    // Clear preferences
                    prefs.edit().clear().apply()

                    // Sync idle state
                    WearDataManager.syncTrackingState(
                        context = ctx,
                        isTracking = false,
                        startElapsed = 0L,
                        targetSec = 0,
                        note = "",
                        categories = emptySet(),
                        categoryDisplays = "",
                        photoUri = null
                    )
                }
            }
            "CANCEL" -> {
                // Stop service
                val stopIntent = Intent(ctx, TrackerService::class.java).apply {
                    action = TrackerService.ACTION_STOP
                }
                ctx.startService(stopIntent)

                // Clear prefs
                prefs.edit().clear().apply()

                // Sync idle state
                WearDataManager.syncTrackingState(
                    context = ctx,
                    isTracking = false,
                    startElapsed = 0L,
                    targetSec = 0,
                    note = "",
                    categories = emptySet(),
                    categoryDisplays = "",
                    photoUri = null
                )
            }
            "ADD_ENTRY" -> {
                serviceScope.launch {
                    val category = json.getString("category")
                    val minutes = json.getInt("minutes")
                    val note = json.optString("note", "")

                    if (category.isNotEmpty() && minutes > 0) {
                        val db = AppDatabase.getInstance(ctx)
                        val repository = Repository(db.entryDao(), db.goalDao(), db.categoryDao())
                        val now = System.currentTimeMillis()
                        val dayRange = Repository.getDayRange(now)
                        val catInfo = repository.getCategoryByName(category)
                        val displayName = catInfo?.displayName ?: category

                        val entry = Entry(
                            date = dayRange.first,
                            category = category,
                            minutes = minutes,
                            note = note,
                            startTime = now - minutes * 60 * 1000L,
                            endTime = now
                        )
                        val eventId = CalendarSyncManager.addEvent(ctx, entry, displayName)
                        repository.insertEntry(entry.copy(calendarEventId = eventId))
                        Log.d(TAG, "Quick log entry added: $category, $minutes mins")
                    }
                }
            }
        }
    }
}
