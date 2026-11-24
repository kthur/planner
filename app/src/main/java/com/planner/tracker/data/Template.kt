package com.planner.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,           // "아침 운동 30분"
    val category: String,       // 카테고리 name key
    val note: String = "",
    val durationMinutes: Int = 0,
    val usageCount: Int = 0,    // 사용 빈도로 정렬
    val createdAt: Long = System.currentTimeMillis()
)
