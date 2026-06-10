package com.planner.tracker.viewmodel

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.planner.tracker.data.AppDatabase
import com.planner.tracker.data.CategoryEntity
import com.planner.tracker.data.CategoryStat
import com.planner.tracker.data.DailyCategoryStat
import com.planner.tracker.data.DailyStat
import com.planner.tracker.data.Entry
import com.planner.tracker.data.Goal
import com.planner.tracker.data.Repository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: Repository

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate

    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear

    private val _currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val currentMonth: StateFlow<Int> = _currentMonth

    val entriesForSelectedDate: StateFlow<List<Entry>>

    val monthlyStats: StateFlow<List<CategoryStat>>

    val dailyStats: StateFlow<List<CategoryStat>>

    val weeklyStats: StateFlow<List<CategoryStat>>

    val weeklyDailyStats: StateFlow<List<DailyStat>>

    val monthlyDailyStats: StateFlow<List<DailyStat>>

    val goals: StateFlow<List<Goal>>

    val categories: StateFlow<List<CategoryEntity>>

    val categoryProgress: StateFlow<Map<String, Pair<Int, Int>>>

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds

    private val _alarmTriggered = MutableStateFlow(false)
    val alarmTriggered: StateFlow<Boolean> = _alarmTriggered

    private var _trackingStartedAt = 0L
    private var trackingJob: Job? = null
    private var timerTargetSec = 0

    val weeklyDailyCategoryStats: StateFlow<List<DailyCategoryStat>>

    val monthlyDailyCategoryMap: StateFlow<Map<Long, List<String>>>

    init {
        val db = AppDatabase.getInstance(application)
        repository = Repository(db.entryDao(), db.goalDao(), db.categoryDao())

        categories = repository.getAllCategories().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        entriesForSelectedDate = _selectedDate.flatMapLatest { date ->
            val dayRange = Repository.getDayRange(date)
            repository.getEntriesByDate(dayRange.first)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val monthRange = combine(_currentYear, _currentMonth) { y, m ->
            Repository.getMonthRange(y, m)
        }

        monthlyStats = monthRange.flatMapLatest { (start, end) ->
            repository.getStatsInRange(start, end)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        dailyStats = _selectedDate.flatMapLatest { date ->
            val dayRange = Repository.getDayRange(date)
            repository.getStatsInRange(dayRange.first, dayRange.second)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        weeklyStats = _selectedDate.flatMapLatest { date ->
            val weekRange = Repository.getWeekRange(date)
            repository.getStatsInRange(weekRange.first, weekRange.second)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        weeklyDailyStats = _selectedDate.flatMapLatest { date ->
            val weekRange = Repository.getWeekRange(date)
            repository.getDailyStatsInRange(weekRange.first, weekRange.second)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        monthlyDailyStats = monthRange.flatMapLatest { (start, end) ->
            repository.getDailyStatsInRange(start, end)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        goals = combine(_currentYear, _currentMonth) { year, month ->
            "${year}-${String.format("%02d", month)}"
        }.flatMapLatest { ym ->
            repository.getGoalsByMonth(ym)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        categoryProgress = goals.combine(monthlyStats) { goals, stats ->
            val statMap = stats.associate { it.category to it.total }
            goals.associate { goal ->
                goal.category to ((statMap[goal.category] ?: 0) to goal.targetMinutes)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        weeklyDailyCategoryStats = _selectedDate.mapLatest { date ->
            val (start, end) = Repository.getWeekRange(date)
            val entries = repository.getEntriesBetweenOnce(start, end)
            entries.groupBy { it.date }.flatMap { (day, dayEntries) ->
                dayEntries.groupBy { it.category }.map { (cat, catEntries) ->
                    DailyCategoryStat(day, cat, catEntries.sumOf { it.minutes })
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        monthlyDailyCategoryMap = monthRange.mapLatest { (start, end) ->
            val entries = repository.getEntriesBetweenOnce(start, end)
            entries.groupBy { it.date }.mapValues { (_, dayEntries) ->
                dayEntries.map { it.category }.distinct()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    }

    fun setSelectedDate(date: Long) {
        _selectedDate.value = date
    }

    fun setCurrentMonth(year: Int, month: Int) {
        _currentYear.value = year
        _currentMonth.value = month
    }

    fun addEntry(category: String, minutes: Int, note: String, startTime: Long = 0, endTime: Long = 0) {
        viewModelScope.launch {
            val dayRange = Repository.getDayRange(if (startTime > 0) startTime else _selectedDate.value)
            repository.insertEntry(
                Entry(
                    date = dayRange.first,
                    category = category,
                    minutes = minutes,
                    note = note,
                    startTime = startTime,
                    endTime = endTime
                )
            )
        }
    }

    fun deleteEntry(entry: Entry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }

    fun updateEntry(entry: Entry) {
        viewModelScope.launch {
            repository.updateEntry(entry)
        }
    }

    fun upsertGoal(goal: Goal) {
        viewModelScope.launch {
            repository.upsertGoal(goal)
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            repository.deleteGoal(id)
        }
    }

    fun startTracking(timerMinutes: String) {
        val now = System.currentTimeMillis()
        val timerSec = timerMinutes.toIntOrNull() ?: 0
        _trackingStartedAt = now
        _elapsedSeconds.value = 0
        _isTracking.value = true
        _alarmTriggered.value = false
        timerTargetSec = timerSec * 60
        val hasTimer = timerSec > 0
        updateTrackingNotification(0, hasTimer, timerSec)
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            while (true) {
                val secs = (System.currentTimeMillis() - _trackingStartedAt) / 1000
                _elapsedSeconds.value = secs
                updateTrackingNotification(secs, hasTimer, timerSec)
                if (timerTargetSec > 0 && secs >= timerTargetSec && !_alarmTriggered.value) {
                    _alarmTriggered.value = true
                    sendTimerNotification()
                }
                delay(1000)
            }
        }
    }

    fun stopTrackingAndSave(category: String, note: String): Pair<Long, Long>? {
        trackingJob?.cancel()
        trackingJob = null
        cancelTrackingNotification()
        val now = System.currentTimeMillis()
        val minutes = ((now - _trackingStartedAt) / 60000).toInt()
        _isTracking.value = false
        _alarmTriggered.value = false
        if (minutes > 0) {
            val dayRange = Repository.getDayRange(_trackingStartedAt)
            val entry = Entry(
                date = dayRange.first,
                category = category,
                minutes = minutes,
                note = note,
                startTime = _trackingStartedAt,
                endTime = now
            )
            viewModelScope.launch { repository.insertEntry(entry) }
            return Pair(_trackingStartedAt, now)
        }
        return null
    }

    fun cancelTracking() {
        trackingJob?.cancel()
        trackingJob = null
        cancelTrackingNotification()
        _isTracking.value = false
        _alarmTriggered.value = false
    }

    fun addCategory(name: String, displayName: String, colorHex: String) {
        viewModelScope.launch {
            repository.upsertCategory(CategoryEntity(name = name, displayName = displayName, colorHex = colorHex))
        }
    }

    fun updateCategory(name: String, displayName: String, colorHex: String) {
        viewModelScope.launch {
            repository.upsertCategory(CategoryEntity(name = name, displayName = displayName, colorHex = colorHex))
        }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch {
            repository.deleteCategory(name)
        }
    }

    fun clearAlarmTriggered() {
        _alarmTriggered.value = false
    }

    private fun updateTrackingNotification(seconds: Long, isTimer: Boolean, timerMinutes: Int) {
        val ctx = getApplication<Application>()
        val h = seconds / 3600; val m = (seconds % 3600) / 60; val s = seconds % 60
        val timeStr = String.format("%02d:%02d:%02d", h, m, s)
        val text = if (isTimer && timerMinutes > 0) {
            val remaining = timerMinutes * 60 - seconds
            if (remaining > 0) "남은 시간: ${remaining / 60}분 ${remaining % 60}초"
            else "시간 초과!"
        } else timeStr

        val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(ctx, "tracking_channel")
            .setContentTitle("현재시간을 측정하고 있습니다")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1002, notification)
    }

    private fun cancelTrackingNotification() {
        val ctx = getApplication<Application>()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(1002)
    }

    private fun sendTimerNotification() {
        val ctx = getApplication<Application>()
        val notification = NotificationCompat.Builder(ctx, "timer_alarm")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("타이머 완료")
            .setContentText("설정한 시간이 되었습니다!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1001, notification)
    }

    fun exportDataAsJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val entries = repository.getEntriesBetweenOnce(0L, Long.MAX_VALUE)
            val allGoals = repository.getAllGoals().stateIn(
                viewModelScope, SharingStarted.Eagerly, emptyList()
            ).value
            val sb = StringBuilder()
            sb.appendLine("{\"entries\":[")
            entries.forEachIndexed { i, e ->
                sb.appendLine("""{"id":${e.id},"date":${e.date},"category":"${e.category}","minutes":${e.minutes},"note":"${e.note.replace("\"", "\\\"")}","startTime":${e.startTime},"endTime":${e.endTime}}${if (i < entries.lastIndex) "," else ""}""")
            }
            sb.appendLine("],\"goals\":[")
            allGoals.forEachIndexed { i, g ->
                sb.appendLine("""{"id":${g.id},"yearMonth":"${g.yearMonth}","category":"${g.category}","description":"${g.description.replace("\"", "\\\"")}","targetMinutes":${g.targetMinutes},"deadline":${g.deadline},"isCompleted":${g.isCompleted}}${if (i < allGoals.lastIndex) "," else ""}""")
            }
            sb.appendLine("]}")
            onResult(sb.toString())
        }
    }

    fun importDataFromJson(json: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val root = JSONObject(json)
                val entriesArray = root.getJSONArray("entries")
                for (i in 0 until entriesArray.length()) {
                    val obj = entriesArray.getJSONObject(i)
                    val entry = Entry(
                        id = obj.getLong("id"),
                        date = obj.getLong("date"),
                        category = obj.getString("category"),
                        minutes = obj.getInt("minutes"),
                        note = obj.optString("note", ""),
                        startTime = obj.getLong("startTime"),
                        endTime = obj.getLong("endTime")
                    )
                    repository.insertEntry(entry)
                }
                val goalsArray = root.optJSONArray("goals")
                if (goalsArray != null) {
                    for (i in 0 until goalsArray.length()) {
                        val obj = goalsArray.getJSONObject(i)
                        val goal = Goal(
                            id = obj.getLong("id"),
                            yearMonth = obj.getString("yearMonth"),
                            category = obj.getString("category"),
                            description = obj.optString("description", ""),
                            targetMinutes = obj.getInt("targetMinutes"),
                            deadline = obj.getLong("deadline"),
                            isCompleted = obj.getBoolean("isCompleted")
                        )
                        repository.upsertGoal(goal)
                    }
                }
                onResult(true, "성공적으로 복원되었습니다 (entries: ${entriesArray.length()})")
            } catch (e: Exception) {
                onResult(false, "복원 실패: ${e.message}")
            }
        }
    }
}
