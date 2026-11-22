package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Dedicated PreLectureReceiver for firing 10-minute pre-lecture heads-up alerts.
 * Double-checks HolidayRangeDao, DailyDayStatusDao, Sunday, and Semester bounds
 * inside goAsync() so alerts are strictly muted on holidays/off-days.
 */
class PreLectureReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val asyncPendingResult = goAsync()
        val appContext = context.applicationContext
        val subjectName = intent?.getStringExtra("subjectName") ?: "Upcoming Lecture"
        val roomNo = intent?.getStringExtra("roomNo") ?: "Assigned LT/Lab"
        val startTime = intent?.getStringExtra("startTime") ?: intent?.getStringExtra("time") ?: ""
        val endTime = intent?.getStringExtra("endTime") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationHelper.createNotificationChannels(appContext)

                val db = AppDatabase.getInstance(appContext)
                val today = LocalDate.now()
                val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val isSunday = (today.dayOfWeek.value == 7)

                val prefs = appContext.getSharedPreferences("caliper_settings", Context.MODE_PRIVATE)
                val semesterStartDate = prefs.getString("semester_start_date", "2026-07-15") ?: "2026-07-15"
                val semesterEndDate = prefs.getString("semester_end_date", "2026-12-15") ?: "2026-12-15"
                val isOutsideSemester = (todayStr < semesterStartDate || todayStr > semesterEndDate)

                val holiday = db.holidayRangeDao().getHolidayForDate(todayStr)
                val dayStatus = db.dailyDayStatusDao().getDayStatusDirect(todayStr)
                val isDeclaredOff = dayStatus?.leaveCategory in listOf("college_holiday", "strike")

                // Suppress alert on holidays, off-days, Sundays, or outside semester bounds
                if (holiday != null || isDeclaredOff || isSunday || isOutsideSemester) {
                    return@launch
                }

                NotificationHelper.showPreLectureHeadsUp(
                    context = appContext,
                    subjectName = subjectName,
                    roomNo = roomNo,
                    startTime = startTime,
                    endTime = endTime
                )
            } finally {
                asyncPendingResult.finish()
            }
        }
    }
}
