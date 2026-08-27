package com.praxis.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedNotebookEntryDao {
    @Query("SELECT * FROM cached_notebook_entries WHERE userId = :userId ORDER BY createdAt DESC")
    fun getEntries(userId: String): Flow<List<CachedNotebookEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<CachedNotebookEntry>)

    @Query("DELETE FROM cached_notebook_entries WHERE userId = :userId")
    suspend fun clearUserEntries(userId: String)
}
