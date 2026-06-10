package com.planner.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: Goal)

    @Query("SELECT * FROM goals ORDER BY id DESC")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE yearMonth = :yearMonth ORDER BY id DESC")
    fun getGoalsByMonth(yearMonth: String): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE category = :categoryName ORDER BY id DESC")
    fun getGoalsByCategory(categoryName: String): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getGoalById(id: Long): Goal?

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: Long)
}
