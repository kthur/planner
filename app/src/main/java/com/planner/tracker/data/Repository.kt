package com.planner.tracker.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class Repository(
    private val entryDao: EntryDao,
    private val goalDao: GoalDao,
    private val categoryDao: CategoryDao
) {

    fun getEntriesByDate(date: Long): Flow<List<Entry>> = entryDao.getEntriesByDate(date)

    fun getEntriesBetween(start: Long, end: Long): Flow<List<Entry>> =
        entryDao.getEntriesBetween(start, end)

    suspend fun getEntriesBetweenOnce(start: Long, end: Long): List<Entry> =
        entryDao.getEntriesBetweenOnce(start, end)

    fun getStatsInRange(start: Long, end: Long): Flow<List<CategoryStat>> =
        entryDao.getStatsInRange(start, end)

    fun getCategoryTotalInRange(categoryName: String, start: Long, end: Long): Flow<Int?> =
        entryDao.getCategoryTotalInRange(categoryName, start, end)

    fun getDailyStatsInRange(start: Long, end: Long): Flow<List<DailyStat>> =
        entryDao.getDailyStatsInRange(start, end)

    suspend fun insertEntry(entry: Entry) = entryDao.insert(entry)

    suspend fun updateEntry(entry: Entry) = entryDao.update(entry)

    suspend fun deleteEntry(entry: Entry) = entryDao.delete(entry)

    suspend fun deleteEntryById(id: Long) = entryDao.deleteById(id)

    fun getAllGoals(): Flow<List<Goal>> = goalDao.getAllGoals()

    fun getGoalsByMonth(yearMonth: String): Flow<List<Goal>> = goalDao.getGoalsByMonth(yearMonth)

    fun getGoalsByCategory(categoryName: String): Flow<List<Goal>> =
        goalDao.getGoalsByCategory(categoryName)

    suspend fun getGoalById(id: Long): Goal? = goalDao.getGoalById(id)

    suspend fun upsertGoal(goal: Goal) = goalDao.upsert(goal)

    suspend fun deleteGoal(id: Long) = goalDao.deleteById(id)

    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAll()

    suspend fun getAllCategoriesOnce(): List<CategoryEntity> = categoryDao.getAllOnce()

    suspend fun getCategoryByName(name: String): CategoryEntity? = categoryDao.getByName(name)

    suspend fun upsertCategory(category: CategoryEntity) = categoryDao.upsert(category)

    suspend fun deleteCategory(name: String) = categoryDao.delete(name)

    companion object {
        fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
            val cal = Calendar.getInstance().apply {
                set(year, month - 1, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            cal.add(Calendar.DAY_OF_MONTH, -1)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis
            return start to end
        }

        fun getDayRange(date: Long): Pair<Long, Long> {
            val cal = Calendar.getInstance().apply {
                timeInMillis = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis
            return start to end
        }

        fun getWeekRange(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            cal.add(Calendar.DAY_OF_WEEK, 6)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis
            return start to end
        }

        fun getDayRange24h(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
            val end = now
            val start = end - 24 * 60 * 60 * 1000
            return start to end
        }
    }
}
