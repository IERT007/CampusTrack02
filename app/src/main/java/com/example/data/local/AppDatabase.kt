package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        QuickNoteEntity::class,
        TestRecordEntity::class,
        CustomActivityEntity::class,
        ExtraSlotEntity::class,
        RescheduleOverrideEntity::class
    ],
    version = 6,
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
    abstract fun testRecordDao(): TestRecordDao
    abstract fun customActivityDao(): CustomActivityDao
    abstract fun extraSlotDao(): ExtraSlotDao
    abstract fun rescheduleOverrideDao(): RescheduleOverrideDao
    fun scheduleDao(): ScheduleDao = extraSlotDao()

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `extra_slots` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `subjectId` INTEGER NOT NULL,
                        `startTime` TEXT NOT NULL,
                        `endTime` TEXT NOT NULL,
                        `roomNo` TEXT NOT NULL,
                        `tag` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `reschedule_overrides` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `slotId` INTEGER NOT NULL,
                        `date` TEXT NOT NULL,
                        `newStartTime` TEXT NOT NULL,
                        `newEndTime` TEXT NOT NULL,
                        `newRoomNo` TEXT
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "campustrack_iert.db"
                )
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): AppDatabase = getDatabase(context)
    }
}
