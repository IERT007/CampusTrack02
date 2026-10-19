package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AcademicTaskDao
import com.example.data.local.dao.AssessmentDao
import com.example.data.local.dao.AttendanceLogDao
import com.example.data.local.dao.CustomActivityDao
import com.example.data.local.dao.DailyDayStatusDao
import com.example.data.local.dao.HolidayRangeDao
import com.example.data.local.dao.MedicalLeaveDao
import com.example.data.local.dao.QuickNoteDao
import com.example.data.local.dao.SubjectDao
import com.example.data.local.dao.TestRecordDao
import com.example.data.local.dao.TimetableSlotDao
import com.example.data.local.entity.AcademicTaskEntity
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.AttendanceLogEntity
import com.example.data.local.entity.CustomActivityEntity
import com.example.data.local.entity.DailyDayStatusEntity
import com.example.data.local.entity.HolidayRangeEntity
import com.example.data.local.entity.MedicalLeaveEntity
import com.example.data.local.entity.QuickNoteEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TestRecordEntity
import com.example.data.local.entity.TimetableSlotEntity

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
        QuickNoteEntity::class,
        TestRecordEntity::class,
        CustomActivityEntity::class
    ],
    version = 5,
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
    abstract fun academicTaskDao(): AcademicTaskDao
    abstract fun quickNoteDao(): QuickNoteDao
    abstract fun testRecordDao(): TestRecordDao
    abstract fun customActivityDao(): CustomActivityDao

    fun holidayDao(): HolidayRangeDao = holidayRangeDao()

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "campustrack_iert.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
