package com.planner.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val yearMonth: String,
    val category: Category,
    val targetMinutes: Int
)
