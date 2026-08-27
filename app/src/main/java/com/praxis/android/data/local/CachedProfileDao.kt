package com.praxis.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedProfileDao {
    @Query("SELECT * FROM cached_profile WHERE id = :userId LIMIT 1")
    fun getProfile(userId: String): Flow<CachedProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: CachedProfile)

    @Query("DELETE FROM cached_profile")
    suspend fun clearAll()
}
