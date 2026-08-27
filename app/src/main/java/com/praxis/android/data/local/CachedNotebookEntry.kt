package com.praxis.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_notebook_entries")
data class CachedNotebookEntry(
    @PrimaryKey val id: String,
    val userId: String,
    val content: String,
    val entryType: String?,
    val domain: String?,
    val tags: String?,
    val createdAt: String,
    val cachedAt: Long = System.currentTimeMillis()
)
