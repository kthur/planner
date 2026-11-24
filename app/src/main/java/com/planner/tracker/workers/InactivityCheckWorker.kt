package com.planner.tracker.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.planner.tracker.R
import com.planner.tracker.data.AppDatabase
import com.planner.tracker.data.Repository
import java.util.Calendar

class InactivityCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cal = Calendar.getInstance()
        // 오후 6시 이후에만 알림
        if (cal.get(Calendar.HOUR_OF_DAY) < 18) return Result.success()

        val db = AppDatabase.getInstance(context)
        val repository = Repository(db.entryDao(), db.goalDao(), db.categoryDao())
        val (start, end) = Repository.getDayRange(System.currentTimeMillis())
        val entries = repository.getEntriesBetweenOnce(start, end)
        
        if (entries.isEmpty()) {
            sendNotification()
        }
        return Result.success()
    }

    private fun sendNotification() {
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "inactivity_check"
        val channel = NotificationChannel(channelId, "비활성 알림", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "오늘 기록이 없을 때 알려줍니다"
        }
        notifManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("오늘 기록이 없어요 📝")
            .setContentText("잠깐 시간을 내서 오늘 활동을 기록해볼까요?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notifManager.notify(2001, notification)
    }
}
