package com.planner.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val name: String,
    val displayName: String,
    val colorHex: String,
    val isDefault: Boolean = false,
    val entryType: String = "DURATION"
)
