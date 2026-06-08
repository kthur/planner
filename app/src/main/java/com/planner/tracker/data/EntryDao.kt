package com.planner.tracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Insert
    suspend fun insert(entry: Entry)

    @Delete
    suspend fun delete(entry: Entry)

    @Query("SELECT * FROM entries WHERE date = :date ORDER BY id ASC")
    fun getEntriesByDate(date: Long): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE date >= :start AND date <= :end")
    fun getEntriesBetween(start: Long, end: Long): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE date >= :start AND date <= :end")
    suspend fun getEntriesBetweenOnce(start: Long, end: Long): List<Entry>

    @Query("SELECT category, SUM(minutes) as total FROM entries WHERE date >= :start AND date <= :end GROUP BY category")
    fun getStatsInRange(start: Long, end: Long): Flow<List<CategoryStat>>

    @Query("SELECT SUM(minutes) FROM entries WHERE date >= :start AND date <= :end AND category = :category")
    fun getCategoryTotalInRange(category: Category, start: Long, end: Long): Flow<Int?>

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}

data class CategoryStat(
    val category: Category,
    val total: Int
)
