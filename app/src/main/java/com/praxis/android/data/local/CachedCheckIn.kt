package com.praxis.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_checkins")
data class CachedCheckIn(
    @PrimaryKey val id: String,
    val userId: String,
    val date: String,
    val completed: Boolean,
    val cachedAt: Long = System.currentTimeMillis()
)
