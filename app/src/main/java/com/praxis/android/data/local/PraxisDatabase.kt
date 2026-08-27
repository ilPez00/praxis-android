package com.praxis.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [
    CachedPost::class,
    CachedGoal::class,
    CachedNotebookEntry::class,
    CachedMessage::class,
    CachedProfile::class,
    CachedCheckIn::class,
    PendingMutation::class,
    PendingUpload::class
], version = 4, exportSchema = false)
abstract class PraxisDatabase : RoomDatabase() {
    abstract fun cachedPostDao(): CachedPostDao
    abstract fun cachedGoalDao(): CachedGoalDao
    abstract fun cachedNotebookEntryDao(): CachedNotebookEntryDao
    abstract fun cachedMessageDao(): CachedMessageDao
    abstract fun cachedProfileDao(): CachedProfileDao
    abstract fun cachedCheckInDao(): CachedCheckInDao
    abstract fun pendingMutationDao(): PendingMutationDao
    abstract fun pendingUploadDao(): PendingUploadDao

    companion object {
        @Volatile private var INSTANCE: PraxisDatabase? = null

        fun getInstance(context: Context): PraxisDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    PraxisDatabase::class.java,
                    "praxis_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
