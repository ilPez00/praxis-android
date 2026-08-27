package com.praxis.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedCheckInDao {
    @Query("SELECT * FROM cached_checkins WHERE userId = :userId ORDER BY date DESC LIMIT 1")
    fun getLatestCheckIn(userId: String): Flow<CachedCheckIn?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkIn: CachedCheckIn)

    @Query("DELETE FROM cached_checkins WHERE userId = :userId")
    suspend fun clearUserCheckIns(userId: String)
}
