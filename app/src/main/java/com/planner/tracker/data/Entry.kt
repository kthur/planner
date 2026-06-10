package com.planner.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,
    val category: String,
    val minutes: Int,
    val note: String = "",
    val startTime: Long = 0,
    val endTime: Long = 0
)

data class DailyCategoryStat(
    val date: Long,
    val category: String,
    val total: Int
)
