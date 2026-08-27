package com.praxis.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_goals")
data class CachedGoal(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val description: String?,
    val parentId: String?,
    val domain: String?,
    val progress: Float,
    val createdAt: String,
    val cachedAt: Long = System.currentTimeMillis()
)
