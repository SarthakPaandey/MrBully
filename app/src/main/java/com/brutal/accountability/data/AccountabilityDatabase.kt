package com.brutal.accountability.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        RestrictedAppEntity::class,
        EventLogEntity::class,
        DailyCheckinEntity::class,
        SemanticNoteEntity::class,
        EpisodicMemoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AccountabilityDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun restrictedAppsDao(): RestrictedAppsDao
    abstract fun eventLogDao(): EventLogDao
    abstract fun dailyCheckinDao(): DailyCheckinDao
    abstract fun semanticNotesDao(): SemanticNotesDao
    abstract fun episodicMemoryDao(): EpisodicMemoryDao

    companion object {
        @Volatile
        private var instance: AccountabilityDatabase? = null

        fun get(context: Context): AccountabilityDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context,
                    AccountabilityDatabase::class.java,
                    "accountability.db"
                )
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
            }
        }
    }
}
