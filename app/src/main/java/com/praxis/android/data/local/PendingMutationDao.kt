package com.praxis.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingMutationDao {
    @Query("SELECT * FROM pending_mutations ORDER BY createdAt ASC")
    fun getAll(): Flow<List<PendingMutation>>

    @Insert
    suspend fun insert(mutation: PendingMutation)

    @Query("DELETE FROM pending_mutations WHERE mutationId = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pending_mutations")
    suspend fun clearAll()
}
