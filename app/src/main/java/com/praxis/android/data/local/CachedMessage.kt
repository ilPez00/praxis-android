package com.praxis.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_messages")
data class CachedMessage(
    @PrimaryKey val id: String,
    val senderId: String,
    val receiverId: String?,
    val roomId: String?,
    val content: String,
    val createdAt: String,
    val cachedAt: Long = System.currentTimeMillis()
)
