package com.praxis.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_posts")
data class CachedPost(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val title: String?,
    val content: String,
    val context: String,
    val createdAt: String,
    val cachedAt: Long = System.currentTimeMillis()
)
