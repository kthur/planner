package com.planner.tracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.planner.tracker.MainActivity
import com.planner.tracker.PlannerApp
import com.planner.tracker.R
import com.planner.tracker.data.Repository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.runBlocking

class GoalWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val app = context.applicationContext as PlannerApp
        val repository = Repository(
            app.database.entryDao(),
            app.database.goalDao()
        )

        val cal = Calendar.getInstance()

        val todayRange = Repository.getDayRange(System.currentTimeMillis())
        val monthRange = Repository.getMonthRange(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1
        )

        val (todayTotal, monthTotal) = runBlocking {
            val todayEntries = repository.getEntriesBetweenOnce(todayRange.first, todayRange.second)
            val monthEntries = repository.getEntriesBetweenOnce(monthRange.first, monthRange.second)
            val t = todayEntries.sumOf { it.minutes }
            val m = monthEntries.sumOf { it.minutes }
            t to m
        }

        val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
        val todayText = "오늘: ${todayTotal}분 (${todayTotal / 60}시간 ${todayTotal % 60}분)"
        val monthText = "이번 달: ${monthTotal}분 (${monthTotal / 60}시간 ${monthTotal % 60}분)"

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.goal_widget_layout)
            views.setTextViewText(R.id.widget_title, "Planner")
            views.setTextViewText(R.id.widget_today, todayText)
            views.setTextViewText(R.id.widget_month_total, monthText)
            views.setTextViewText(R.id.widget_goal_progress, "갱신: ${dateFormat.format(Date())}")

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
