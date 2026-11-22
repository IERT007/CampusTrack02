package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.SemesterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Query("SELECT * FROM semesters ORDER BY isActive DESC, id ASC")
    fun getAllSemesters(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters ORDER BY isActive DESC, id ASC")
    suspend fun getAllSemestersDirect(): List<SemesterEntity>

    @Query("SELECT * FROM semesters WHERE isActive = 1 LIMIT 1")
    fun getActiveSemester(): Flow<SemesterEntity?>

    @Query("SELECT * FROM semesters WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSemesterDirect(): SemesterEntity?

    @Query("SELECT * FROM semesters WHERE id = :id LIMIT 1")
    suspend fun getSemesterById(id: Long): SemesterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemester(semester: SemesterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemesters(semesters: List<SemesterEntity>)

    @Update
    suspend fun updateSemester(semester: SemesterEntity)

    @Query("UPDATE semesters SET isActive = 0")
    suspend fun deactivateAllSemesters()

    @Query("UPDATE semesters SET isActive = 1 WHERE id = :semesterId")
    suspend fun activateSemester(semesterId: Long)

    @Transaction
    suspend fun switchActiveSemester(semesterId: Long) {
        deactivateAllSemesters()
        activateSemester(semesterId)
    }

    @Query("UPDATE semesters SET isArchived = :isArchived WHERE id = :semesterId")
    suspend fun setSemesterArchived(semesterId: Long, isArchived: Boolean)

    @Query("UPDATE semesters SET startDate = :startDate, endDate = :endDate WHERE id = :semesterId")
    suspend fun updateSemesterDates(semesterId: Long, startDate: String, endDate: String)

    @Query("DELETE FROM semesters WHERE id = :semesterId")
    suspend fun deleteSemesterById(semesterId: Long)

    @Query("DELETE FROM semesters")
    suspend fun deleteAllSemesters()
}
