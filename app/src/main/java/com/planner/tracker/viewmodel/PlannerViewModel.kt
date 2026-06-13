package com.planner.tracker.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.SystemClock
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
import com.planner.tracker.data.CalendarSyncManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
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

    private val _restoredNote = MutableStateFlow("")
    val restoredNote: StateFlow<String> = _restoredNote

    private val _restoredCategories = MutableStateFlow<Set<String>>(emptySet())
    val restoredCategories: StateFlow<Set<String>> = _restoredCategories

    private val _restoredPhotoUri = MutableStateFlow<String?>(null)
    val restoredPhotoUri: StateFlow<String?> = _restoredPhotoUri

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
            val statMap = stats.associate { it.category to it.totalMinutes }
            goals.associate { goal ->
                goal.category to ((statMap[goal.category] ?: 0) to goal.targetMinutes)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        weeklyDailyCategoryStats = _selectedDate.mapLatest { date ->
            val (start, end) = Repository.getWeekRange(date)
            val entries = repository.getEntriesBetweenOnce(start, end)
            entries.groupBy { it.date }.flatMap { (day, dayEntries) ->
                dayEntries.groupBy { it.category }.map { (cat, catEntries) ->
                    DailyCategoryStat(day, cat, catEntries.sumOf { it.minutes }, catEntries.sumOf { it.count })
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        monthlyDailyCategoryMap = monthRange.mapLatest { (start, end) ->
            val entries = repository.getEntriesBetweenOnce(start, end)
            entries.groupBy { it.date }.mapValues { (_, dayEntries) ->
                dayEntries.map { it.category }.distinct()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        // Load tracking state from SharedPreferences
        val prefs = application.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
        val isTrackingRestore = prefs.getBoolean("is_tracking", false)
        if (isTrackingRestore) {
            val startElapsed = prefs.getLong("start_elapsed", 0L)
            val targetSec = prefs.getInt("target_sec", 0)
            val note = prefs.getString("note", "") ?: ""
            val categoriesString = prefs.getString("categories", "") ?: ""
            val categoriesSet = if (categoriesString.isEmpty()) emptySet() else categoriesString.split(",").toSet()
            val timerMinutes = prefs.getString("timer_minutes", "") ?: ""

            _trackingStartedAt = startElapsed
            _isTracking.value = true
            timerTargetSec = targetSec
            _restoredNote.value = note
            _restoredCategories.value = categoriesSet
            _restoredPhotoUri.value = prefs.getString("photo_uri", null)

            val timerSec = timerMinutes.toIntOrNull() ?: 0
            trackingJob = viewModelScope.launch {
                while (true) {
                    val secs = (SystemClock.elapsedRealtime() - _trackingStartedAt) / 1000
                    if (timerTargetSec > 0 && secs >= timerTargetSec) {
                        _elapsedSeconds.value = timerTargetSec.toLong()
                        if (!_alarmTriggered.value) {
                            _alarmTriggered.value = true
                        }
                        break
                    }
                    _elapsedSeconds.value = secs
                    delay(1000)
                }
            }
        }
    }

    fun setSelectedDate(date: Long) {
        _selectedDate.value = date
    }

    fun setCurrentMonth(year: Int, month: Int) {
        _currentYear.value = year
        _currentMonth.value = month
    }

    fun addEntry(category: String, minutes: Int, note: String, startTime: Long = 0, endTime: Long = 0, entryType: String = "DURATION", count: Int = 0, photoUri: String? = null) {
        viewModelScope.launch {
            val dayRange = Repository.getDayRange(if (startTime > 0) startTime else _selectedDate.value)
            val catInfo = repository.getCategoryByName(category)
            val displayName = catInfo?.displayName ?: category

            val tempEntry = Entry(
                date = dayRange.first,
                category = category,
                minutes = minutes,
                note = note,
                startTime = startTime,
                endTime = endTime,
                entryType = entryType,
                count = count,
                photoUri = photoUri
            )

            val eventId = CalendarSyncManager.addEvent(getApplication(), tempEntry, displayName)
            repository.insertEntry(tempEntry.copy(calendarEventId = eventId))
        }
    }

    fun deleteEntry(entry: Entry) {
        viewModelScope.launch {
            if (entry.calendarEventId != null) {
                CalendarSyncManager.deleteEvent(getApplication(), entry.calendarEventId)
            }
            if (!entry.photoUri.isNullOrEmpty()) {
                val file = java.io.File(java.io.File(getApplication<Application>().filesDir, "photos"), entry.photoUri)
                if (file.exists()) file.delete()
            }
            repository.deleteEntry(entry)
        }
    }

    fun updateEntry(entry: Entry) {
        viewModelScope.launch {
            val oldEntry = repository.getEntryById(entry.id)
            if (oldEntry != null && !oldEntry.photoUri.isNullOrEmpty() && oldEntry.photoUri != entry.photoUri) {
                val file = java.io.File(java.io.File(getApplication<Application>().filesDir, "photos"), oldEntry.photoUri)
                if (file.exists()) file.delete()
            }

            val catInfo = repository.getCategoryByName(entry.category)
            val displayName = catInfo?.displayName ?: entry.category

            var newEventId = entry.calendarEventId
            if (entry.calendarEventId != null) {
                val success = CalendarSyncManager.updateEvent(getApplication(), entry.calendarEventId, entry, displayName)
                if (!success) {
                    newEventId = CalendarSyncManager.addEvent(getApplication(), entry, displayName)
                }
            } else {
                newEventId = CalendarSyncManager.addEvent(getApplication(), entry, displayName)
            }

            repository.updateEntry(entry.copy(calendarEventId = newEventId))
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

    fun startTracking(categories: Set<String>, categoryDisplays: String, note: String, timerMinutes: String, photoUri: String? = null) {
        val now = SystemClock.elapsedRealtime()
        val timerSec = timerMinutes.toIntOrNull() ?: 0
        _trackingStartedAt = now
        _elapsedSeconds.value = 0
        _isTracking.value = true
        _alarmTriggered.value = false
        timerTargetSec = timerSec * 60

        _restoredNote.value = note
        _restoredCategories.value = categories
        _restoredPhotoUri.value = photoUri

        val prefs = getApplication<Application>().getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_tracking", true)
            putLong("start_elapsed", now)
            putInt("target_sec", timerTargetSec)
            putString("timer_minutes", timerMinutes)
            putString("note", note)
            putString("categories", categories.joinToString(","))
            putString("category_displays", categoryDisplays)
            putString("photo_uri", photoUri)
        }.apply()

        val ctx = getApplication<Application>()
        val serviceIntent = Intent(ctx, com.planner.tracker.TrackerService::class.java).apply {
            action = com.planner.tracker.TrackerService.ACTION_START
            putExtra(com.planner.tracker.TrackerService.EXTRA_NOTE, note)
            putExtra(com.planner.tracker.TrackerService.EXTRA_CATEGORIES, categories.joinToString(","))
            putExtra(com.planner.tracker.TrackerService.EXTRA_CATEGORY_DISPLAYS, categoryDisplays)
            putExtra(com.planner.tracker.TrackerService.EXTRA_TIMER_MINUTES, timerMinutes)
        }
        try {
            ctx.startForegroundService(serviceIntent)
        } catch (e: Exception) {
            ctx.startService(serviceIntent)
        }

        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            while (true) {
                val secs = (SystemClock.elapsedRealtime() - _trackingStartedAt) / 1000
                if (timerTargetSec > 0 && secs >= timerTargetSec) {
                    _elapsedSeconds.value = timerTargetSec.toLong()
                    if (!_alarmTriggered.value) {
                        _alarmTriggered.value = true
                    }
                    break
                }
                _elapsedSeconds.value = secs
                delay(1000)
            }
        }
    }

    fun stopTrackingAndSave(categories: Set<String>, note: String, photoUri: String? = null): Pair<Long, Long>? {
        trackingJob?.cancel()
        trackingJob = null
        val ctx = getApplication<Application>()
        val stopIntent = Intent(ctx, com.planner.tracker.TrackerService::class.java).apply {
            action = com.planner.tracker.TrackerService.ACTION_STOP
        }
        ctx.startService(stopIntent)

        val now = System.currentTimeMillis()
        val startWallClock = now - (SystemClock.elapsedRealtime() - _trackingStartedAt)
        val actualSeconds = (SystemClock.elapsedRealtime() - _trackingStartedAt) / 1000
        val savedSeconds = if (timerTargetSec > 0) minOf(actualSeconds, timerTargetSec.toLong()) else actualSeconds
        val minutes = (savedSeconds / 60).toInt()
        val endTimeValue = if (timerTargetSec > 0 && actualSeconds > timerTargetSec) startWallClock + timerTargetSec * 1000L else now
        _isTracking.value = false
        _alarmTriggered.value = false
        clearTrackingPrefs()
        if (minutes > 0 && categories.isNotEmpty()) {
            val dayRange = Repository.getDayRange(startWallClock)
            viewModelScope.launch {
                categories.forEach { category ->
                    val catInfo = repository.getCategoryByName(category)
                    val displayName = catInfo?.displayName ?: category

                    val tempEntry = Entry(
                        date = dayRange.first,
                        category = category,
                        minutes = minutes,
                        note = note,
                        startTime = startWallClock,
                        endTime = endTimeValue,
                        photoUri = photoUri
                    )

                    val eventId = CalendarSyncManager.addEvent(getApplication(), tempEntry, displayName)
                    repository.insertEntry(tempEntry.copy(calendarEventId = eventId))
                }
            }
            return Pair(startWallClock, endTimeValue)
        }
        return null
    }

    fun cancelTracking() {
        trackingJob?.cancel()
        trackingJob = null
        val ctx = getApplication<Application>()
        val stopIntent = Intent(ctx, com.planner.tracker.TrackerService::class.java).apply {
            action = com.planner.tracker.TrackerService.ACTION_STOP
        }
        ctx.startService(stopIntent)
        _isTracking.value = false
        _alarmTriggered.value = false
        clearTrackingPrefs()
    }

    fun updateTrackingNote(note: String) {
        _restoredNote.value = note
        val ctx = getApplication<Application>()
        val prefs = ctx.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("note", note).apply()
        val intent = Intent(ctx, com.planner.tracker.TrackerService::class.java).apply {
            action = com.planner.tracker.TrackerService.ACTION_UPDATE_NOTE
            putExtra(com.planner.tracker.TrackerService.EXTRA_NOTE, note)
        }
        ctx.startService(intent)
    }

    fun updateTrackingCategories(categories: Set<String>, categoryDisplays: String) {
        _restoredCategories.value = categories
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, com.planner.tracker.TrackerService::class.java).apply {
            action = com.planner.tracker.TrackerService.ACTION_UPDATE_CATEGORIES
            putExtra(com.planner.tracker.TrackerService.EXTRA_CATEGORIES, categories.joinToString(","))
            putExtra(com.planner.tracker.TrackerService.EXTRA_CATEGORY_DISPLAYS, categoryDisplays)
        }
        ctx.startService(intent)
    }

    private fun clearTrackingPrefs() {
        val prefs = getApplication<Application>().getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        _restoredNote.value = ""
        _restoredCategories.value = emptySet()
        _restoredPhotoUri.value = null
    }

    fun addCategory(name: String, displayName: String, colorHex: String, entryType: String = "DURATION") {
        viewModelScope.launch {
            repository.upsertCategory(CategoryEntity(name = name, displayName = displayName, colorHex = colorHex, entryType = entryType))
        }
    }

    fun updateCategory(name: String, displayName: String, colorHex: String, entryType: String = "DURATION") {
        viewModelScope.launch {
            repository.upsertCategory(CategoryEntity(name = name, displayName = displayName, colorHex = colorHex, entryType = entryType))
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

    fun exportDataAsJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val entries = repository.getEntriesBetweenOnce(0L, Long.MAX_VALUE)
            val allGoals = repository.getAllGoals().firstOrNull() ?: emptyList()
            val sb = StringBuilder()
            sb.appendLine("{\"entries\":[")
            entries.forEachIndexed { i, e ->
                val photoVal = if (e.photoUri != null) "\"${e.photoUri}\"" else "null"
                sb.appendLine("""{"id":${e.id},"date":${e.date},"category":"${e.category}","minutes":${e.minutes},"note":"${e.note.replace("\"", "\\\"")}","startTime":${e.startTime},"endTime":${e.endTime},"photoUri":$photoVal}${if (i < entries.lastIndex) "," else ""}""")
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
                        endTime = obj.getLong("endTime"),
                        photoUri = if (obj.isNull("photoUri")) null else obj.optString("photoUri").takeIf { it.isNotEmpty() }
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
