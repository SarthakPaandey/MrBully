package com.brutal.accountability.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val nickname: String,
    val profession: String,
    val goal: String,
    val insecurity: String,
    val fear: String,
    val leverageJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "restricted_apps")
data class RestrictedAppEntity(
    @PrimaryKey val packageName: String,
    val label: String
)

@Entity(tableName = "event_logs")
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val atMillis: Long,
    val withHeadphones: Boolean
)

@Entity(tableName = "daily_checkins")
data class DailyCheckinEntity(
    @PrimaryKey val dateIso: String,
    val morningPlan: String,
    val nightReflection: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "semantic_notes")
data class SemanticNoteEntity(
    @PrimaryKey val id: String,
    val type: String,
    val weight: Int,
    val content: String,
    val source: String,
    val tagsJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "episodic_memories")
data class EpisodicMemoryEntity(
    @PrimaryKey val id: String,
    val type: String = "episodic_failure",
    val durationMinutes: Int,
    val restrictedAppsJson: String,
    val totalOpens: Int,
    val headline: String,
    val userReaction: String,
    val occurredAt: Long = System.currentTimeMillis()
)

@Dao
interface EpisodicMemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: EpisodicMemoryEntity)

    @Query("SELECT * FROM episodic_memories ORDER BY occurredAt DESC LIMIT 20")
    fun observeRecent(): kotlinx.coroutines.flow.Flow<List<EpisodicMemoryEntity>>
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserProfileEntity)
}

@Dao
interface RestrictedAppsDao {
    @Query("SELECT * FROM restricted_apps")
    fun observeAll(): Flow<List<RestrictedAppEntity>>

    @Query("SELECT packageName FROM restricted_apps")
    suspend fun getPackageNames(): List<String>

    @Query("DELETE FROM restricted_apps")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<RestrictedAppEntity>)
}

@Dao
interface EventLogDao {
    @Query("SELECT * FROM event_logs ORDER BY atMillis DESC LIMIT 100")
    fun observeRecent(): Flow<List<EventLogEntity>>

    @Insert
    suspend fun insert(entity: EventLogEntity)

    @Query("DELETE FROM event_logs WHERE atMillis < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long): Int
}

@Dao
interface DailyCheckinDao {
    @Query("SELECT * FROM daily_checkins WHERE dateIso = :dateIso")
    suspend fun getByDate(dateIso: String): DailyCheckinEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyCheckinEntity)
}

@Dao
interface SemanticNotesDao {
    @Query("SELECT * FROM semantic_notes ORDER BY createdAt DESC LIMIT 50")
    fun observeRecent(): Flow<List<SemanticNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SemanticNoteEntity)
}
