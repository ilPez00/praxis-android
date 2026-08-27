package com.praxis.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedMessageDao {
    @Query("SELECT * FROM cached_messages WHERE roomId = :roomId OR (roomId IS NULL AND (senderId = :user1 OR senderId = :user2)) ORDER BY createdAt ASC")
    fun getMessages(roomId: String?, user1: String, user2: String): Flow<List<CachedMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<CachedMessage>)

    @Query("DELETE FROM cached_messages")
    suspend fun clearAll()
}
