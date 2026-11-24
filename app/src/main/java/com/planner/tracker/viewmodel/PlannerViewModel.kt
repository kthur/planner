package com.planner.tracker.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import android.content.Intent
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.planner.tracker.data.CloudService
import com.planner.tracker.data.AppDatabase
import com.planner.tracker.data.CategoryEntity
import com.planner.tracker.data.CategoryStat
import com.planner.tracker.data.DailyCategoryStat
import com.planner.tracker.data.DailyStat
import com.planner.tracker.data.Entry
import com.planner.tracker.data.Goal
import com.planner.tracker.data.Repository
import com.planner.tracker.data.CalendarSyncManager
import com.planner.tracker.data.WearDataManager
import com.planner.tracker.data.NetworkMonitor
import com.planner.tracker.data.Template
import com.planner.tracker.data.TemplateDao
import com.planner.tracker.data.HourStat
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModel(application: Application) : AndroidViewModel(application) {

    // ─── SharedPreferences 키 상수 ───────────────────────────────────────────
    companion object {
        private const val PREFS_TRACKER        = "tracker_prefs"
        private const val PREFS_GOAL_REMINDER  = "goal_reminder_prefs"

        private const val KEY_IS_TRACKING       = "is_tracking"
        private const val KEY_START_ELAPSED     = "start_elapsed"
        private const val KEY_TARGET_SEC        = "target_sec"
        private const val KEY_TIMER_MINUTES     = "timer_minutes"
        private const val KEY_NOTE              = "note"
        private const val KEY_CATEGORIES        = "categories"
        private const val KEY_CATEGORY_DISPLAYS = "category_displays"
        private const val KEY_PHOTO_URI         = "photo_uri"
        private const val KEY_LAST_NOTIFIED_KEY = "last_notified_key"  // "YYYY-DDD" 형식
    }

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

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak

    private val _longestStreak = MutableStateFlow(0)
    val longestStreak: StateFlow<Int> = _longestStreak

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

    private val templateDao: TemplateDao
    val templates: StateFlow<List<Template>>
    val hourlyStats: StateFlow<List<HourStat>>

    // Firebase states
    val currentUserState = CloudService.getAuthStateFlow().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val userProfileState = currentUserState.flatMapLatest { user ->
        if (user != null) {
            CloudService.getUserProfileFlow(user.uid)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val sharedFeed = userProfileState.flatMapLatest { profile ->
        if (profile?.groupId != null) {
            CloudService.subscribeToFeed(profile.groupId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val db = AppDatabase.getInstance(application)
        repository = Repository(db.entryDao(), db.goalDao(), db.categoryDao())
        templateDao = db.templateDao()

        templates = templateDao.getAll().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        hourlyStats = _selectedDate.flatMapLatest { date ->
            val dayRange = Repository.getDayRange(date)
            repository.getHourlyStatsInRange(dayRange.first, dayRange.second)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        categories = repository.getAllCategories().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        viewModelScope.launch {
            categories.collect { catList ->
                WearDataManager.syncCategories(application, catList)
            }
        }

        viewModelScope.launch {
            userProfileState.collect { profile ->
                if (profile?.groupId != null) {
                    syncUnsyncedEntries()
                }
            }
        }

        viewModelScope.launch {
            NetworkMonitor(application).isOnline.collect { online ->
                if (online) {
                    syncUnsyncedEntries()
                }
            }
        }

        viewModelScope.launch {
            val todayRange = Repository.getDayRange(System.currentTimeMillis())
            repository.getEntriesByDate(todayRange.first).collect { todayEntries ->
                val totalMinutes = todayEntries.sumOf { it.minutes }
                WearDataManager.syncTodayTotalMinutes(application, totalMinutes)
            }
        }

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

        // DB 변경 시 자동 갱신: mapLatest(1회성) → flatMapLatest + Flow 기반 DAO 쿼리
        weeklyDailyCategoryStats = _selectedDate.flatMapLatest { date ->
            val (start, end) = Repository.getWeekRange(date)
            repository.getDailyCategoryStatsInRange(start, end)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // monthlyDailyCategoryMap도 Flow 기반으로 교체
        monthlyDailyCategoryMap = monthRange.flatMapLatest { (start, end) ->
            repository.getDailyCategoryStatsInRange(start, end)
        }.mapLatest { stats ->
            stats.groupBy { it.date }.mapValues { (_, dayStats) ->
                dayStats.map { it.category }.distinct()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        viewModelScope.launch {
            entriesForSelectedDate.collect { refreshStreak() }
        }

        // Load tracking state from SharedPreferences
        val prefs = application.getSharedPreferences(PREFS_TRACKER, Context.MODE_PRIVATE)
        val isTrackingRestore = prefs.getBoolean(KEY_IS_TRACKING, false)
        if (isTrackingRestore) {
            val startElapsed = prefs.getLong(KEY_START_ELAPSED, 0L)
            val targetSec = prefs.getInt(KEY_TARGET_SEC, 0)
            val note = prefs.getString(KEY_NOTE, "") ?: ""
            val categoriesString = prefs.getString(KEY_CATEGORIES, "") ?: ""
            val categoriesSet = if (categoriesString.isEmpty()) emptySet() else categoriesString.split(",").toSet()
            val timerMinutes = prefs.getString(KEY_TIMER_MINUTES, "") ?: ""

            _trackingStartedAt = startElapsed
            _isTracking.value = true
            timerTargetSec = targetSec
            _restoredNote.value = note
            _restoredCategories.value = categoriesSet
            _restoredPhotoUri.value = prefs.getString(KEY_PHOTO_URI, null)

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
            val finalEntry = tempEntry.copy(calendarEventId = eventId)
            val insertedId = repository.insertEntry(finalEntry)
            syncEntryToCloud(finalEntry.copy(id = insertedId))

            // Firebase sharing if user belongs to a group and a photo is attached
            val user = currentUserState.value
            val profile = userProfileState.value
            val groupId = profile?.groupId
            if (groupId != null && user != null && photoUri != null) {
                // Upload photo to Firebase Storage
                val localUri = Uri.parse("file://${getApplication<Application>().filesDir}/photos/${photoUri}")
                val uploadResult = CloudService.uploadPhoto(groupId, localUri)
                val photoUrl = uploadResult.getOrNull()
                // Prepare shared entry
                val sharedEntry = CloudService.SharedEntry(
                    id = java.util.UUID.randomUUID().toString(),
                    category = category,
                    minutes = minutes,
                    note = note,
                    photoUrl = photoUrl,
                    userId = user.uid,
                    userName = user.displayName ?: "",
                    date = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
                CloudService.shareEntry(sharedEntry, groupId)
            }
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

            val profile = userProfileState.value
            if (profile?.groupId != null) {
                CloudService.deleteSyncedEntry(profile.groupId, entry.id)
            }
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

            val updatedLocal = entry.copy(calendarEventId = newEventId, isSynced = false)
            repository.updateEntry(updatedLocal)
            syncEntryToCloud(updatedLocal)
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

        val prefs = getApplication<Application>().getSharedPreferences(PREFS_TRACKER, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_IS_TRACKING, true)
            putLong(KEY_START_ELAPSED, now)
            putInt(KEY_TARGET_SEC, timerTargetSec)
            putString(KEY_TIMER_MINUTES, timerMinutes)
            putString(KEY_NOTE, note)
            putString(KEY_CATEGORIES, categories.joinToString(","))
            putString(KEY_CATEGORY_DISPLAYS, categoryDisplays)
            putString(KEY_PHOTO_URI, photoUri)
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

        WearDataManager.syncTrackingState(
            context = ctx,
            isTracking = true,
            startElapsed = now,
            targetSec = timerTargetSec,
            note = note,
            categories = categories,
            categoryDisplays = categoryDisplays,
            photoUri = photoUri
        )

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
                    val finalEntry = tempEntry.copy(calendarEventId = eventId)
                    val insertedId = repository.insertEntry(finalEntry)
                    syncEntryToCloud(finalEntry.copy(id = insertedId))
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

    fun updateTrackingNote(note: String) {
        _restoredNote.value = note
        val ctx = getApplication<Application>()
        val prefs = ctx.getSharedPreferences(PREFS_TRACKER, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NOTE, note).apply()
        val intent = Intent(ctx, com.planner.tracker.TrackerService::class.java).apply {
            action = com.planner.tracker.TrackerService.ACTION_UPDATE_NOTE
            putExtra(com.planner.tracker.TrackerService.EXTRA_NOTE, note)
        }
        ctx.startService(intent)

        val isTracking = prefs.getBoolean(KEY_IS_TRACKING, false)
        val startElapsed = prefs.getLong(KEY_START_ELAPSED, 0L)
        val targetSec = prefs.getInt(KEY_TARGET_SEC, 0)
        val categoriesString = prefs.getString(KEY_CATEGORIES, "") ?: ""
        val categoriesSet = if (categoriesString.isEmpty()) emptySet() else categoriesString.split(",").toSet()
        val categoryDisplays = prefs.getString(KEY_CATEGORY_DISPLAYS, "") ?: ""
        val photoUri = prefs.getString(KEY_PHOTO_URI, null)

        WearDataManager.syncTrackingState(
            context = ctx,
            isTracking = isTracking,
            startElapsed = startElapsed,
            targetSec = targetSec,
            note = note,
            categories = categoriesSet,
            categoryDisplays = categoryDisplays,
            photoUri = photoUri
        )
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

        val prefs = ctx.getSharedPreferences(PREFS_TRACKER, Context.MODE_PRIVATE)
        val isTracking = prefs.getBoolean(KEY_IS_TRACKING, false)
        val startElapsed = prefs.getLong(KEY_START_ELAPSED, 0L)
        val targetSec = prefs.getInt(KEY_TARGET_SEC, 0)
        val note = prefs.getString(KEY_NOTE, "") ?: ""
        val photoUri = prefs.getString(KEY_PHOTO_URI, null)

        WearDataManager.syncTrackingState(
            context = ctx,
            isTracking = isTracking,
            startElapsed = startElapsed,
            targetSec = targetSec,
            note = note,
            categories = categories,
            categoryDisplays = categoryDisplays,
            photoUri = photoUri
        )
    }

    private fun clearTrackingPrefs() {
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_TRACKER, Context.MODE_PRIVATE)
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

    fun refreshStreak() {
        viewModelScope.launch {
            val dates = repository.getAllDistinctDates()
            val days = dates.map { ts ->
                val cal = Calendar.getInstance().apply { timeInMillis = ts }
                Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            }.distinct().sortedWith(compareByDescending<Triple<Int,Int,Int>> { it.first }
                .thenByDescending { it.second }
                .thenByDescending { it.third })

            // 현재 스트릭: 오늘부터 역방향 연속 계산
            var streak = 0
            val checkCal = Calendar.getInstance()
            for ((y, m, d) in days) {
                if (checkCal.get(Calendar.YEAR) == y &&
                    checkCal.get(Calendar.MONTH) == m &&
                    checkCal.get(Calendar.DAY_OF_MONTH) == d) {
                    streak++
                    checkCal.add(Calendar.DAY_OF_MONTH, -1)
                } else break
            }
            _currentStreak.value = streak

            // 최장 스트릭 계산
            if (days.isEmpty()) { _longestStreak.value = 0; return@launch }
            var longest = 1
            var current = 1
            for (i in 1 until days.size) {
                val (py, pm, pd) = days[i - 1]
                val (cy, cm, cd) = days[i]
                val prev = Calendar.getInstance().apply { set(py, pm, pd) }
                prev.add(Calendar.DAY_OF_MONTH, -1)
                if (prev.get(Calendar.YEAR) == cy && prev.get(Calendar.MONTH) == cm &&
                    prev.get(Calendar.DAY_OF_MONTH) == cd) {
                    current++
                } else {
                    longest = maxOf(longest, current)
                    current = 1
                }
            }
            _longestStreak.value = maxOf(longest, current)
        }
    }

    fun addTemplate(name: String, category: String, note: String, durationMinutes: Int) {
        viewModelScope.launch {
            templateDao.insert(
                Template(
                    name = name,
                    category = category,
                    note = note,
                    durationMinutes = durationMinutes
                )
            )
        }
    }

    fun deleteTemplate(template: Template) {
        viewModelScope.launch {
            templateDao.delete(template)
        }
    }

    fun useTemplate(template: Template) {
        viewModelScope.launch {
            templateDao.incrementUsage(template.id)
        }
    }

    fun shareStatsCard(context: Context, dateText: String, totalMinutes: Int, streak: Int, topCategory: String?) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val width = 800
                val height = 1000
                val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)

                // 배경 채우기
                val bgPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#181825")
                    style = android.graphics.Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                // 카드 영역
                val cardPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#252538")
                    style = android.graphics.Paint.Style.FILL
                    isAntiAlias = true
                }
                val rectF = android.graphics.RectF(50f, 60f, (width - 50).toFloat(), (height - 60).toFloat())
                canvas.drawRoundRect(rectF, 40f, 40f, cardPaint)

                val textPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                // 앱 타이틀
                textPaint.textSize = 32f
                textPaint.color = android.graphics.Color.parseColor("#A6ADC8")
                canvas.drawText("PLANNER · 집중 기록", width / 2f, 150f, textPaint)

                // 날짜
                textPaint.textSize = 42f
                textPaint.isFakeBoldText = true
                textPaint.color = android.graphics.Color.WHITE
                canvas.drawText(dateText, width / 2f, 230f, textPaint)

                // 구분선
                val linePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#45475A")
                    strokeWidth = 3f
                }
                canvas.drawLine(120f, 280f, (width - 120).toFloat(), 280f, linePaint)

                // 총 시간
                val hours = totalMinutes / 60
                val mins = totalMinutes % 60
                val timeStr = if (hours > 0) "${hours}시간 ${mins}분" else "${mins}분"

                textPaint.textSize = 34f
                textPaint.isFakeBoldText = false
                textPaint.color = android.graphics.Color.parseColor("#BAC2DE")
                canvas.drawText("총 집중 시간", width / 2f, 380f, textPaint)

                textPaint.textSize = 68f
                textPaint.isFakeBoldText = true
                textPaint.color = android.graphics.Color.parseColor("#89B4FA")
                canvas.drawText(timeStr, width / 2f, 470f, textPaint)

                // 스트릭
                if (streak > 0) {
                    textPaint.textSize = 38f
                    textPaint.isFakeBoldText = true
                    textPaint.color = android.graphics.Color.parseColor("#FAB387")
                    canvas.drawText("🔥 $streak 일 연속 기록 중!", width / 2f, 570f, textPaint)
                }

                // 주 활동 카테고리
                if (!topCategory.isNullOrBlank()) {
                    textPaint.textSize = 34f
                    textPaint.isFakeBoldText = false
                    textPaint.color = android.graphics.Color.parseColor("#CDD6F4")
                    canvas.drawText("가장 많은 활동: $topCategory", width / 2f, 660f, textPaint)
                }

                // 하단 워터마크
                textPaint.textSize = 28f
                textPaint.color = android.graphics.Color.parseColor("#585B70")
                textPaint.isFakeBoldText = false
                canvas.drawText("Planner 앱에서 기록됨", width / 2f, 850f, textPaint)

                // 파일 저장
                val shareDir = java.io.File(context.cacheDir, "share")
                if (!shareDir.exists()) shareDir.mkdirs()
                val imageFile = java.io.File(shareDir, "planner_share_${System.currentTimeMillis()}.png")
                java.io.FileOutputStream(imageFile).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                }

                val authority = "${context.packageName}.fileprovider"
                val contentUri = androidx.core.content.FileProvider.getUriForFile(context, authority, imageFile)

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, "오늘 $timeStr 동안 집중했어요! 🎯 #Planner #시간기록")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(shareIntent, "통계 카드 공유하기").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun checkAndNotifyGoalReminder() {
        val ctx = getApplication<Application>()
        val prefs = ctx.getSharedPreferences(PREFS_GOAL_REMINDER, Context.MODE_PRIVATE)
        // DAY_OF_YEAR 단독 사용 시 연도가 달라도 같은 값 → "YYYY-DDD" 조합으로 고유화
        val cal = Calendar.getInstance()
        val todayKey = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        val lastNotifiedKey = prefs.getString(KEY_LAST_NOTIFIED_KEY, "") ?: ""
        if (todayKey == lastNotifiedKey) return

        viewModelScope.launch {
            val goals = repository.getAllGoals().firstOrNull() ?: return@launch
            val stats = monthlyStats.value
            val statMap = stats.associate { it.category to it.totalMinutes }
            val incomplete = goals.filter { !it.isCompleted && it.targetMinutes > 0 && (statMap[it.category] ?: 0) < it.targetMinutes }
            if (incomplete.isEmpty()) return@launch

            val notifManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "goal_reminder"
            val channel = NotificationChannel(channelId, "목표 리마인더", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "달성하지 못한 목표를 알려줍니다"
            }
            notifManager.createNotificationChannel(channel)

            val summary = incomplete.joinToString(", ") { it.category }
            val notification = NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("목표 알림")
                .setContentText("${incomplete.size}개의 목표를 달성하지 못했습니다: $summary")
                .setStyle(NotificationCompat.BigTextStyle().bigText("${incomplete.size}개의 목표가 아직 달성되지 않았습니다.\n\n목표: $summary\n\n오늘 기록을 추가해보세요!"))
                .setAutoCancel(true)
                .build()

            notifManager.notify(1001, notification)
            prefs.edit().putString(KEY_LAST_NOTIFIED_KEY, todayKey).apply()
        }
    }

    fun exportDataAsCsv(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val entries = repository.getEntriesBetweenOnce(0L, Long.MAX_VALUE)
            val sb = StringBuilder()
            sb.appendLine("date,category,minutes,count,entryType,note,startTime,endTime")
            entries.forEach { e ->
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(e.date))
                sb.appendLine("${dateStr},${e.category},${e.minutes},${e.count},${e.entryType},\"${e.note.replace("\"", "\"\"")}\",${e.startTime},${e.endTime}")
            }
            onResult(sb.toString())
        }
    }

    fun exportDataAsJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val entries = repository.getEntriesBetweenOnce(0L, Long.MAX_VALUE)
                val allGoals = repository.getAllGoals().firstOrNull() ?: emptyList()

                // JSONObject/JSONArray 사용 → 특수문자 escaping 버그 방지
                val entriesArray = JSONArray()
                entries.forEach { e ->
                    val obj = JSONObject().apply {
                        put("id", e.id)
                        put("date", e.date)
                        put("category", e.category)
                        put("minutes", e.minutes)
                        put("note", e.note)
                        put("startTime", e.startTime)
                        put("endTime", e.endTime)
                        put("entryType", e.entryType)
                        put("count", e.count)
                        if (e.photoUri != null) put("photoUri", e.photoUri) else put("photoUri", JSONObject.NULL)
                    }
                    entriesArray.put(obj)
                }

                val goalsArray = JSONArray()
                allGoals.forEach { g ->
                    val obj = JSONObject().apply {
                        put("id", g.id)
                        put("yearMonth", g.yearMonth)
                        put("category", g.category)
                        put("description", g.description)
                        put("targetMinutes", g.targetMinutes)
                        put("targetCount", g.targetCount)
                        put("periodType", g.periodType)
                        put("deadline", g.deadline)
                        put("isCompleted", g.isCompleted)
                    }
                    goalsArray.put(obj)
                }

                val root = JSONObject().apply {
                    put("entries", entriesArray)
                    put("goals", goalsArray)
                }
                onResult(root.toString(2))
            } catch (e: Exception) {
                onResult("{\"error\":\"내보내기 실패: ${e.message}\"}")
            }
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
                        startTime = obj.optLong("startTime", 0L),
                        endTime = obj.optLong("endTime", 0L),
                        entryType = obj.optString("entryType", "DURATION"),
                        count = obj.optInt("count", 0),
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
                            targetMinutes = obj.optInt("targetMinutes", 0),
                            targetCount = obj.optInt("targetCount", 0),
                            periodType = obj.optString("periodType", "MONTHLY"),
                            deadline = obj.optLong("deadline", 0L),
                            isCompleted = obj.optBoolean("isCompleted", false)
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

    fun registerUser(email: String, password: String, displayName: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = CloudService.registerUser(email, password, displayName)
            if (result.isSuccess) {
                onResult(true, "회원가입 성공")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "회원가입 실패")
            }
        }
    }

    fun loginUser(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = CloudService.loginUser(email, password)
            if (result.isSuccess) {
                onResult(true, "로그인 성공")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "로그인 실패")
            }
        }
    }

    fun logoutUser() {
        CloudService.logout()
    }

    fun createGroup(name: String, onResult: (Boolean, String) -> Unit) {
        val user = currentUserState.value
        if (user == null) {
            onResult(false, "로그인이 필요합니다.")
            return
        }
        viewModelScope.launch {
            val result = CloudService.createGroup(name, user.uid)
            if (result.isSuccess) {
                onResult(true, result.getOrThrow())
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "그룹 생성 실패")
            }
        }
    }

    fun joinGroup(code: String, onResult: (Boolean, String) -> Unit) {
        val user = currentUserState.value
        if (user == null) {
            onResult(false, "로그인이 필요합니다.")
            return
        }
        viewModelScope.launch {
            val result = CloudService.joinGroup(code, user.uid)
            if (result.isSuccess) {
                onResult(true, result.getOrThrow())
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "그룹 가입 실패")
            }
        }
    }

    fun leaveGroup(code: String, onResult: (Boolean, String) -> Unit) {
        val user = currentUserState.value
        if (user == null) {
            onResult(false, "로그인이 필요합니다.")
            return
        }
        viewModelScope.launch {
            val result = CloudService.leaveGroup(user.uid, code)
            if (result.isSuccess) {
                onResult(true, "그룹 탈퇴 성공")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "그룹 탈퇴 실패")
            }
        }
    }

    fun syncEntryToCloud(entry: Entry) {
        val user = currentUserState.value ?: return
        val profile = userProfileState.value ?: return
        val groupId = profile.groupId ?: return

        viewModelScope.launch {
            try {
                var finalEntry = entry
                // 1. If photoUri is local and photoUrl is null, upload it first
                if (!entry.photoUri.isNullOrEmpty() && entry.photoUrl.isNullOrEmpty()) {
                    val file = java.io.File(java.io.File(getApplication<Application>().filesDir, "photos"), entry.photoUri)
                    if (file.exists()) {
                        val localUri = Uri.fromFile(file)
                        val uploadResult = CloudService.uploadPhoto(groupId, localUri)
                        if (uploadResult.isSuccess) {
                            val downloadUrl = uploadResult.getOrThrow()
                            finalEntry = entry.copy(photoUrl = downloadUrl)
                            repository.updateEntry(finalEntry)
                        }
                    }
                }

                // 2. Sync entry metadata to Firestore
                val syncResult = CloudService.syncEntry(
                    groupId = groupId,
                    entry = finalEntry,
                    userId = user.uid,
                    userName = profile.displayName.ifEmpty { user.email ?: "멤버" }
                )

                if (syncResult.isSuccess) {
                    repository.updateEntry(finalEntry.copy(isSynced = true))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun syncUnsyncedEntries() {
        val profile = userProfileState.value ?: return
        if (profile.groupId.isNullOrEmpty()) return

        viewModelScope.launch {
            try {
                val unsynced = repository.getUnsyncedEntries()
                unsynced.forEach { entry ->
                    syncEntryToCloud(entry)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addReaction(entryId: String, emoji: String) {
        val user = currentUserState.value ?: return
        val profile = userProfileState.value ?: return
        val groupId = profile.groupId ?: return

        viewModelScope.launch {
            CloudService.addReaction(
                groupId = groupId,
                entryId = entryId,
                userId = user.uid,
                emoji = emoji
            )
        }
    }
}
