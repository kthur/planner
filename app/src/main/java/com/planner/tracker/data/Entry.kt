package com.planner.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,
    val category: Category,
    val minutes: Int,
    val note: String = "",
    val startTime: Long = 0,
    val endTime: Long = 0
)

data class DailyCategoryStat(
    val date: Long,
    val category: Category,
    val total: Int
)

enum class Category(val displayName: String) {
    HEALTH("운동"),
    MIND("독서"),
    FAMILY("가족"),
    LANGUAGE("외국어"),
    FINANCE("재테크"),
    TECHNOLOGY("기술")
}
