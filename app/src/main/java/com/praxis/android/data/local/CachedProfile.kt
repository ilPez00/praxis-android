package com.praxis.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_profile")
data class CachedProfile(
    @PrimaryKey val id: String,
    val email: String,
    val name: String,
    val bio: String?,
    val avatarUrl: String?,
    val streak: Int,
    val cachedAt: Long = System.currentTimeMillis()
)
