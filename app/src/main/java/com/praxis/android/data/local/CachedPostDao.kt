package com.praxis.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedPostDao {
    @Query("SELECT * FROM cached_posts ORDER BY cachedAt DESC")
    fun getPosts(): Flow<List<CachedPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<CachedPost>)

    @Query("DELETE FROM cached_posts")
    suspend fun clearAll()
}
