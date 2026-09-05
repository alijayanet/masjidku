package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MosqueConfigDao {
    @Query("SELECT * FROM mosque_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<MosqueConfigEntity?>

    @Query("SELECT * FROM mosque_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): MosqueConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: MosqueConfigEntity)
}

@Dao
interface FinanceDao {
    @Query("SELECT * FROM finance_transactions ORDER BY id DESC")
    fun getAllTransactionsFlow(): Flow<List<FinanceEntity>>

    @Query("SELECT * FROM finance_transactions ORDER BY id DESC")
    suspend fun getAllTransactions(): List<FinanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: FinanceEntity): Long

    @Update
    suspend fun update(transaction: FinanceEntity)

    @Query("DELETE FROM finance_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM finance_transactions")
    suspend fun deleteAll()
}

@Dao
interface FridayOfficerDao {
    @Query("SELECT * FROM friday_schedules ORDER BY id ASC")
    fun getAllSchedulesFlow(): Flow<List<FridayOfficerEntity>>

    @Query("SELECT * FROM friday_schedules WHERE id = :id LIMIT 1")
    fun getScheduleFlow(id: Long = 1): Flow<FridayOfficerEntity?>

    @Query("SELECT * FROM friday_schedules WHERE id = :id LIMIT 1")
    suspend fun getSchedule(id: Long = 1): FridayOfficerEntity?

    @Query("SELECT * FROM friday_schedules ORDER BY id ASC")
    suspend fun getAllSchedules(): List<FridayOfficerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(schedule: FridayOfficerEntity)
}

@Dao
interface MosqueActivityDao {
    @Query("SELECT * FROM mosque_activities ORDER BY id DESC")
    fun getAllActivitiesFlow(): Flow<List<MosqueActivityEntity>>

    @Query("SELECT * FROM mosque_activities ORDER BY id DESC")
    suspend fun getAllActivities(): List<MosqueActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: MosqueActivityEntity): Long

    @Update
    suspend fun update(activity: MosqueActivityEntity)

    @Query("DELETE FROM mosque_activities WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface RunningTextDao {
    @Query("SELECT * FROM running_texts WHERE isActive = 1 ORDER BY itemOrder ASC, id ASC")
    fun getActiveRunningTextsFlow(): Flow<List<RunningTextEntity>>

    @Query("SELECT * FROM running_texts ORDER BY itemOrder ASC, id ASC")
    fun getAllRunningTextsFlow(): Flow<List<RunningTextEntity>>

    @Query("SELECT * FROM running_texts ORDER BY itemOrder ASC, id ASC")
    suspend fun getAllRunningTexts(): List<RunningTextEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RunningTextEntity): Long

    @Update
    suspend fun update(item: RunningTextEntity)

    @Query("DELETE FROM running_texts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM running_texts")
    suspend fun deleteAll()
}

@Dao
interface MediaSlideDao {
    @Query("SELECT * FROM media_slides WHERE isActive = 1 ORDER BY itemOrder ASC, id ASC")
    fun getActiveSlidesFlow(): Flow<List<MediaSlideEntity>>

    @Query("SELECT * FROM media_slides ORDER BY itemOrder ASC, id ASC")
    fun getAllSlidesFlow(): Flow<List<MediaSlideEntity>>

    @Query("SELECT * FROM media_slides ORDER BY itemOrder ASC, id ASC")
    suspend fun getAllSlides(): List<MediaSlideEntity>

    @Query("SELECT * FROM media_slides WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MediaSlideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(slide: MediaSlideEntity): Long

    @Update
    suspend fun update(slide: MediaSlideEntity)

    @Query("DELETE FROM media_slides WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface DailyImamScheduleDao {
    @Query("SELECT * FROM daily_imam_schedules ORDER BY date ASC, id ASC")
    fun getAllSchedulesFlow(): Flow<List<DailyImamScheduleEntity>>

    @Query("SELECT * FROM daily_imam_schedules ORDER BY date ASC, id ASC")
    suspend fun getAllSchedules(): List<DailyImamScheduleEntity>

    @Query("SELECT * FROM daily_imam_schedules WHERE date = :date AND prayerName = :prayerName LIMIT 1")
    suspend fun getScheduleByDateAndPrayer(date: String, prayerName: String): DailyImamScheduleEntity?

    @Query("SELECT * FROM daily_imam_schedules WHERE date = :date ORDER BY id ASC")
    fun getSchedulesForDateFlow(date: String): Flow<List<DailyImamScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: DailyImamScheduleEntity): Long

    @Update
    suspend fun update(schedule: DailyImamScheduleEntity)

    @Query("DELETE FROM daily_imam_schedules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM daily_imam_schedules WHERE date < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: String)
}

@Dao
interface MurottalAudioDao {
    @Query("SELECT * FROM murottal_audio_items ORDER BY id DESC")
    fun getAllAudiosFlow(): Flow<List<MurottalAudioEntity>>

    @Query("SELECT * FROM murottal_audio_items ORDER BY id DESC")
    suspend fun getAllAudios(): List<MurottalAudioEntity>

    @Query("SELECT * FROM murottal_audio_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MurottalAudioEntity?

    @Query("SELECT * FROM murottal_audio_items WHERE prayerTime = :prayer OR prayerTime = 'ALL' ORDER BY isDefault DESC, id DESC")
    suspend fun getForPrayer(prayer: String): List<MurottalAudioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(audio: MurottalAudioEntity): Long

    @Update
    suspend fun update(audio: MurottalAudioEntity)

    @Query("DELETE FROM murottal_audio_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE murottal_audio_items SET isDefault = 0")
    suspend fun clearAllDefaults()

    @Query("UPDATE murottal_audio_items SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultById(id: Long)
}

