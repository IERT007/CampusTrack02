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
        SemesterEntity::class,
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
        RescheduleOverrideEntity::class,
        TodoTaskEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun semesterDao(): SemesterDao
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
    abstract fun todoDao(): TodoDao
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

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `todo_tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subjectId` INTEGER,
                        `dueDateTimeMillis` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        /**
         * Explicit non-destructive migration from v7 -> v8:
         * - Creates `semesters` table and seeds default Semester 3 profile (id = 1)
         * - Adds `semesterId INTEGER NOT NULL DEFAULT 1` to all 7 semester-scoped tables
         *   so 100% of existing 3rd Semester attendance data is preserved intact.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `semesters` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `startDate` TEXT NOT NULL,
                        `endDate` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT OR IGNORE INTO `semesters` (`id`, `name`, `startDate`, `endDate`, `isActive`, `isArchived`)
                    VALUES (1, 'Semester 3 • Tool Engg', '2026-09-19', '2026-12-15', 1, 0)
                """.trimIndent())

                db.execSQL("ALTER TABLE `subjects` ADD COLUMN `semesterId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `timetable_slots` ADD COLUMN `semesterId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `attendance_logs` ADD COLUMN `semesterId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `extra_slots` ADD COLUMN `semesterId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `reschedule_overrides` ADD COLUMN `semesterId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `assessments` ADD COLUMN `semesterId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `todo_tasks` ADD COLUMN `semesterId` INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val seedDefaultSemesterCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("""
                    INSERT OR IGNORE INTO `semesters` (`id`, `name`, `startDate`, `endDate`, `isActive`, `isArchived`)
                    VALUES (1, 'Semester 3 • Tool Engg', '2026-09-19', '2026-12-15', 1, 0)
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
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .addCallback(seedDefaultSemesterCallback)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): AppDatabase = getDatabase(context)
    }
}
