package com.planner.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.planner.tracker.data.AppDatabase
import com.planner.tracker.data.Category
import com.planner.tracker.data.CategoryStat
import com.planner.tracker.data.DailyStat
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

    val categoryProgress: StateFlow<Map<Category, Pair<Int, Int>>>

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

        monthlyDailyStats = combine(_currentYear, _currentMonth) { year, month ->
            Repository.getMonthRange(year, month)
        }.flatMapLatest { (start, end) ->
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

    fun exportDataAsJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val entries = repository.getEntriesBetweenOnce(0L, Long.MAX_VALUE)
            val allGoals = repository.getAllGoals().stateIn(
                viewModelScope, SharingStarted.Eagerly, emptyList()
            ).value
            val sb = StringBuilder()
            sb.appendLine("{\"entries\":[")
            entries.forEachIndexed { i, e ->
                sb.appendLine("""{"id":${e.id},"date":${e.date},"category":"${e.category.name}","minutes":${e.minutes},"note":"${e.note.replace("\"", "\\\"")}","startTime":${e.startTime},"endTime":${e.endTime}}${if (i < entries.lastIndex) "," else ""}""")
            }
            sb.appendLine("],\"goals\":[")
            allGoals.forEachIndexed { i, g ->
                sb.appendLine("""{"id":${g.id},"yearMonth":"${g.yearMonth}","category":"${g.category.name}","description":"${g.description.replace("\"", "\\\"")}","targetMinutes":${g.targetMinutes},"deadline":${g.deadline},"isCompleted":${g.isCompleted}}${if (i < allGoals.lastIndex) "," else ""}""")
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
                        category = Category.valueOf(obj.getString("category")),
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
                            category = Category.valueOf(obj.getString("category")),
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
