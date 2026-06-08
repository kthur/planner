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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

    init {
        val db = AppDatabase.getInstance(application)
        repository = Repository(db.entryDao(), db.goalDao())

        val dayRange = Repository.getDayRange(_selectedDate.value)
        entriesForSelectedDate = repository.getEntriesByDate(dayRange.first).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        val monthRange = Repository.getMonthRange(_currentYear.value, _currentMonth.value)
        monthlyStats = repository.getStatsInRange(monthRange.first, monthRange.second).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        val day24h = Repository.getDayRange24h()
        dailyStats = repository.getStatsInRange(day24h.first, day24h.second).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        val weekRange = Repository.getWeekRange()
        weeklyStats = repository.getStatsInRange(weekRange.first, weekRange.second).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        goals = repository.getGoalsByMonth(
            "${_currentYear.value}-${String.format("%02d", _currentMonth.value)}"
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        categoryProgress = goals.combine(monthlyStats) { goals, stats ->
            val statMap = stats.associate { it.category to it.total }
            goals.associate { goal ->
                goal.category to ((statMap[goal.category] ?: 0) to goal.targetMinutes)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    }

    fun setSelectedDate(date: Long) {
        _selectedDate.value = date
        val range = Repository.getDayRange(date)
        viewModelScope.launch {
            repository.getEntriesByDate(range.first).collect { }
        }
    }

    fun setCurrentMonth(year: Int, month: Int) {
        _currentYear.value = year
        _currentMonth.value = month
        refreshMonthData()
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

    fun refreshMonthData() {
        val monthRange = Repository.getMonthRange(_currentYear.value, _currentMonth.value)
        viewModelScope.launch {
            repository.getStatsInRange(monthRange.first, monthRange.second).collect { }
        }
        viewModelScope.launch {
            repository.getGoalsByMonth(
                "${_currentYear.value}-${String.format("%02d", _currentMonth.value)}"
            ).collect { }
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
