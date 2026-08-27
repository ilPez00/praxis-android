package com.praxis.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingUploadDao {

    @Query("SELECT * FROM pending_uploads ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<PendingUpload>>

    @Query("SELECT * FROM pending_uploads ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingUpload>

    @Insert
    suspend fun insert(upload: PendingUpload): Long

    @Query("DELETE FROM pending_uploads WHERE uploadId = :uploadId")
    suspend fun delete(uploadId: Long)

    @Query("SELECT COUNT(*) FROM pending_uploads")
    suspend fun count(): Int
}
