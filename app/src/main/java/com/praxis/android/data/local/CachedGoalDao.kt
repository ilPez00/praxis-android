package com.praxis.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedGoalDao {
    @Query("SELECT * FROM cached_goals WHERE userId = :userId ORDER BY createdAt DESC")
    fun getGoals(userId: String): Flow<List<CachedGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<CachedGoal>)

    @Query("DELETE FROM cached_goals WHERE userId = :userId")
    suspend fun clearUserGoals(userId: String)
}
