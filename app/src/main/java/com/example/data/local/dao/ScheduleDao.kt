package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.ExtraSlotEntity
import com.example.data.local.entity.RescheduleOverrideEntity
import kotlinx.coroutines.flow.Flow

/**
 * ExtraSlotDao — Manages date-locked ad-hoc extra class slots.
 */
@Dao
interface ExtraSlotDao {
    @Query("SELECT * FROM extra_slots WHERE date = :date ORDER BY startTime ASC")
    fun getExtraSlotsForDate(date: String): Flow<List<ExtraSlotEntity>>

    @Query("SELECT * FROM extra_slots WHERE date = :date ORDER BY startTime ASC")
    suspend fun getExtraSlotsForDateDirect(date: String): List<ExtraSlotEntity>

    @Query("SELECT * FROM extra_slots ORDER BY date ASC, startTime ASC")
    fun getAllExtraSlots(): Flow<List<ExtraSlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtraSlot(slot: ExtraSlotEntity): Long

    @Update
    suspend fun updateExtraSlot(slot: ExtraSlotEntity)

    @Delete
    suspend fun deleteExtraSlot(slot: ExtraSlotEntity)

    @Query("DELETE FROM extra_slots WHERE id = :id")
    suspend fun deleteExtraSlotById(id: Long)

    @Query("DELETE FROM extra_slots WHERE date = :date")
    suspend fun deleteExtraSlotsForDate(date: String)

    @Query("DELETE FROM extra_slots")
    suspend fun deleteAllExtraSlots()
}

/**
 * RescheduleOverrideDao — Manages single-day time shifts without touching master timetable.
 */
@Dao
interface RescheduleOverrideDao {
    @Query("SELECT * FROM reschedule_overrides WHERE date = :date")
    fun getOverridesForDate(date: String): Flow<List<RescheduleOverrideEntity>>

    @Query("SELECT * FROM reschedule_overrides WHERE date = :date")
    suspend fun getOverridesForDateDirect(date: String): List<RescheduleOverrideEntity>

    @Query("SELECT * FROM reschedule_overrides")
    fun getAllOverrides(): Flow<List<RescheduleOverrideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverride(override: RescheduleOverrideEntity): Long

    @Update
    suspend fun updateOverride(override: RescheduleOverrideEntity)

    @Delete
    suspend fun deleteOverride(override: RescheduleOverrideEntity)

    @Query("DELETE FROM reschedule_overrides WHERE slotId = :slotId AND date = :date")
    suspend fun deleteOverrideForSlot(slotId: Long, date: String)

    @Query("DELETE FROM reschedule_overrides WHERE id = :id")
    suspend fun deleteOverrideById(id: Long)

    @Query("DELETE FROM reschedule_overrides")
    suspend fun deleteAllOverrides()
}

typealias ScheduleDao = ExtraSlotDao
