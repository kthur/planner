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

    @Query("SELECT * FROM goals WHERE yearMonth = :yearMonth")
    fun getGoalsByMonth(yearMonth: String): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE yearMonth = :yearMonth AND category = :category LIMIT 1")
    suspend fun getGoal(yearMonth: String, category: Category): Goal?

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: Long)
}
