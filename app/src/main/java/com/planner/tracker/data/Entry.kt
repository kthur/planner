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

enum class Category(val displayName: String) {
    HEALTH("Health"),
    MIND("Mind"),
    FAMILY("Family"),
    LANGUAGE("Language"),
    FINANCE("Finance"),
    TECHNOLOGY("Technology")
}
