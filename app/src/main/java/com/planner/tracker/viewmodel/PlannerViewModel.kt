package com.planner.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.planner.tracker.data.AppDatabase
import com.planner.tracker.data.Category
import com.planner.tracker.data.CategoryStat
import com.planner.tracker.data.Entry
import com.planner.tracker.data.Goal
import com.planner.tracker.data.Repository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

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

    val goals: StateFlow<List<Goal>>

    val categoryProgress: StateFlow<Map<Category, Pair<Int, Int>>>

    @OptIn(ExperimentalCoroutinesApi::class)
    init {
        val db = AppDatabase.getInstance(application)
        repository = Repository(db.entryDao(), db.goalDao())

        entriesForSelectedDate = _selectedDate.flatMapLatest { date ->
            val dayRange = Repository.getDayRange(date)
            repository.getEntriesByDate(dayRange.first)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        monthlyStats = combine(_currentYear, _currentMonth) { year, month ->
            Repository.getMonthRange(year, month)
        }.flatMapLatest { (start, end) ->
            repository.getStatsInRange(start, end)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        dailyStats = repository.getStatsInRange(
            Repository.getDayRange24h().first,
            Repository.getDayRange24h().second
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        weeklyStats = repository.getStatsInRange(
            Repository.getWeekRange().first,
            Repository.getWeekRange().second
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    }

    fun setSelectedDate(date: Long) {
        _selectedDate.value = date
    }

    fun setCurrentMonth(year: Int, month: Int) {
        _currentYear.value = year
        _currentMonth.value = month
    }

    fun addEntry(category: Category, minutes: Int, note: String, startTime: Long = 0, endTime: Long = 0) {
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

    fun refreshTimeRanges() {
        val day24h = Repository.getDayRange24h()
        viewModelScope.launch {
            repository.getStatsInRange(day24h.first, day24h.second).collect { }
        }
        val weekRange = Repository.getWeekRange()
        viewModelScope.launch {
            repository.getStatsInRange(weekRange.first, weekRange.second).collect { }
        }
    }
}
