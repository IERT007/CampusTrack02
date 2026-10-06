package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        SubjectEntity::class,
        TimetableSlotEntity::class,
        AttendanceLogEntity::class,
        AssessmentEntity::class,
        DailyDayStatusEntity::class,
        MedicalLeaveEntity::class,
        HolidayRangeEntity::class,
        AcademicTaskEntity::class,
        QuickNoteEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableSlotDao(): TimetableSlotDao
    abstract fun attendanceLogDao(): AttendanceLogDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun dailyDayStatusDao(): DailyDayStatusDao
    abstract fun medicalLeaveDao(): MedicalLeaveDao
    abstract fun holidayRangeDao(): HolidayRangeDao
    fun holidayDao(): HolidayRangeDao = holidayRangeDao()
    abstract fun academicTaskDao(): AcademicTaskDao
    abstract fun quickNoteDao(): QuickNoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "campustrack_iert.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): AppDatabase = getDatabase(context)
    }
}
