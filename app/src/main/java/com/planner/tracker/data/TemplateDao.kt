package com.planner.tracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: Template): Long

    @Update
    suspend fun update(template: Template)

    @Delete
    suspend fun delete(template: Template)

    @Query("SELECT * FROM templates ORDER BY usageCount DESC, createdAt DESC")
    fun getAll(): Flow<List<Template>>

    @Query("UPDATE templates SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsage(id: Long)
}
