package com.planner.tracker

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TrackerService : Service() {

    companion object {
        const val ACTION_START = "com.planner.tracker.START"
        const val ACTION_STOP = "com.planner.tracker.STOP"
        const val ACTION_UPDATE_NOTE = "com.planner.tracker.UPDATE_NOTE"
        const val ACTION_UPDATE_CATEGORIES = "com.planner.tracker.UPDATE_CATEGORIES"

        const val EXTRA_NOTE = "note"
        const val EXTRA_CATEGORIES = "categories"
        const val EXTRA_CATEGORY_DISPLAYS = "category_displays"
        const val EXTRA_TIMER_MINUTES = "timer_minutes"

        private var _isRunning = false
        val isRunning: Boolean get() = _isRunning

        private val _elapsedSeconds = MutableStateFlow(0L)
        val elapsedSeconds: StateFlow<Long> = _elapsedSeconds

        private var startElapsed = 0L
        private var timerTargetSec = 0
        private var serviceJob: Job? = null
        private var serviceScope: CoroutineScope? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val note = intent.getStringExtra(EXTRA_NOTE) ?: ""
                val categories = intent.getStringExtra(EXTRA_CATEGORIES) ?: ""
                val categoryDisplays = intent.getStringExtra(EXTRA_CATEGORY_DISPLAYS) ?: ""
                val timerMinutes = intent.getStringExtra(EXTRA_TIMER_MINUTES) ?: ""

                val prefs = getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
                val storedStartTime = prefs.getLong("start_elapsed", 0L)

                startElapsed = if (storedStartTime > 0 && prefs.getBoolean("is_tracking", false)) {
                    storedStartTime
                } else {
                    SystemClock.elapsedRealtime()
                }
                timerTargetSec = (timerMinutes.toIntOrNull() ?: 0) * 60

                prefs.edit().apply {
                    putBoolean("is_tracking", true)
                    putLong("start_elapsed", startElapsed)
                    putInt("target_sec", timerTargetSec)
                    putString(EXTRA_TIMER_MINUTES, timerMinutes)
                    putString(EXTRA_NOTE, note)
                    putString(EXTRA_CATEGORIES, categories)
                    putString(EXTRA_CATEGORY_DISPLAYS, categoryDisplays)
                }.apply()

                _isRunning = true
                startForeground(1002, buildNotification(0))

                serviceScope?.cancel()
                val scope = CoroutineScope(Dispatchers.Default + Job())
                serviceScope = scope
                serviceJob = scope.launch {
                    while (true) {
                        val elapsed = (SystemClock.elapsedRealtime() - startElapsed) / 1000
                        if (timerTargetSec > 0 && elapsed >= timerTargetSec) {
                            _elapsedSeconds.value = timerTargetSec.toLong()
                            updateNotification(timerTargetSec.toLong())
                            sendTimerAlarmNotification()
                            break
                        }
                        _elapsedSeconds.value = elapsed
                        updateNotification(elapsed)
                        delay(1000)
                    }
                }
            }
            ACTION_STOP -> {
                stopTimer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE_NOTE -> {
                val note = intent.getStringExtra(EXTRA_NOTE) ?: ""
                getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
                    .edit().putString(EXTRA_NOTE, note).apply()
                val elapsed = (SystemClock.elapsedRealtime() - startElapsed) / 1000
                updateNotification(elapsed)
            }
            ACTION_UPDATE_CATEGORIES -> {
                val categories = intent.getStringExtra(EXTRA_CATEGORIES) ?: ""
                val displays = intent.getStringExtra(EXTRA_CATEGORY_DISPLAYS) ?: ""
                getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
                    .edit().putString(EXTRA_CATEGORIES, categories)
                    .putString(EXTRA_CATEGORY_DISPLAYS, displays).apply()
                val elapsed = (SystemClock.elapsedRealtime() - startElapsed) / 1000
                updateNotification(elapsed)
            }
        }
        return START_STICKY
    }

    private fun stopTimer() {
        serviceJob?.cancel()
        serviceJob = null
        serviceScope?.cancel()
        serviceScope = null
        _isRunning = false
        _elapsedSeconds.value = 0
        getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    private fun buildNotification(seconds: Long): Notification {
        val h = seconds / 3600; val m = (seconds % 3600) / 60; val s = seconds % 60
        val timeStr = String.format("%02d:%02d:%02d", h, m, s)

        val prefs = getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
        val note = prefs.getString(EXTRA_NOTE, "") ?: ""
        val categoryDisplays = prefs.getString(EXTRA_CATEGORY_DISPLAYS, "") ?: ""
        val timerMins = prefs.getString(EXTRA_TIMER_MINUTES, "") ?: ""
        val timerSec = (timerMins.toIntOrNull() ?: 0) * 60

        val title = if (categoryDisplays.isNotEmpty()) "[$categoryDisplays] 측정 중"
            else "시간 측정 중"

        val desc = if (timerSec > 0) {
            val remaining = (timerSec - seconds).coerceAtLeast(0)
            if (remaining > 0) "남은 시간: ${remaining / 60}분 ${remaining % 60}초"
            else "⏰ 시간 초과!"
        } else timeStr

        val bigText = buildString {
            appendLine("경과: $timeStr")
            if (categoryDisplays.isNotEmpty()) appendLine("카테고리: $categoryDisplays")
            if (note.isNotEmpty()) appendLine("메모: $note")
            if (timerSec > 0) {
                val remaining = (timerSec - seconds).coerceAtLeast(0)
                if (remaining > 0) append("남은 시간: ${remaining / 60}분 ${remaining % 60}초")
                else append("⏰ 시간 초과!")
            }
        }

        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, "tracking_channel")
            .setContentTitle(title)
            .setContentText(desc)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText.toString().trimEnd()))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(seconds: Long) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1002, buildNotification(seconds))
    }

    private fun sendTimerAlarmNotification() {
        val notification = NotificationCompat.Builder(this, "timer_alarm")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏰ 타이머 완료")
            .setContentText("설정한 시간이 되었습니다!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1001, notification)
    }

    override fun onDestroy() {
        stopTimer()
        super.onDestroy()
    }
}
