package com.planner.tracker.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class Repository(private val entryDao: EntryDao, private val goalDao: GoalDao) {

    fun getEntriesByDate(date: Long): Flow<List<Entry>> = entryDao.getEntriesByDate(date)

    fun getEntriesBetween(start: Long, end: Long): Flow<List<Entry>> =
        entryDao.getEntriesBetween(start, end)

    suspend fun getEntriesBetweenOnce(start: Long, end: Long): List<Entry> =
        entryDao.getEntriesBetweenOnce(start, end)

    fun getMonthlyStats(start: Long, end: Long): Flow<List<CategoryStat>> =
        entryDao.getMonthlyStats(start, end)

    fun getCategoryTotalInRange(category: Category, start: Long, end: Long): Flow<Int?> =
        entryDao.getCategoryTotalInRange(category, start, end)

    suspend fun insertEntry(entry: Entry) = entryDao.insert(entry)

    suspend fun deleteEntry(entry: Entry) = entryDao.delete(entry)

    suspend fun deleteEntryById(id: Long) = entryDao.deleteById(id)

    fun getGoalsByMonth(yearMonth: String): Flow<List<Goal>> = goalDao.getGoalsByMonth(yearMonth)

    suspend fun getGoal(yearMonth: String, category: Category): Goal? =
        goalDao.getGoal(yearMonth, category)

    suspend fun upsertGoal(goal: Goal) = goalDao.upsert(goal)

    suspend fun deleteGoal(id: Long) = goalDao.deleteById(id)

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
    }
}
