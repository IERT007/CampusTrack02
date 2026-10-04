package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val asyncPendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(appContext)
                val today = LocalDate.now()
                val dayOfWeek = today.dayOfWeek.value // 1 = Monday, 7 = Sunday

                // Check Institutional Closures and Holidays
                val holiday = db.holidayDao().getHolidayForDate(today.toString())
                if (holiday != null || dayOfWeek == 7) {
                    NotificationHelper.postQuietNotice(
                        context = appContext,
                        title = "Campus Closed",
                        message = holiday?.title ?: "Sunday - No academic periods scheduled."
                    )
                    return@launch
                }

                // Query Active Scheduled Classes
                val activeSlots = db.timetableSlotDao().getSlotsForDayDirect(dayOfWeek)
                if (activeSlots.isEmpty()) {
                    NotificationHelper.postQuietNotice(
                        context = appContext,
                        title = "Free Day",
                        message = "No timetable lectures mapped for today."
                    )
                    return@launch
                }

                val firstClass = activeSlots.minByOrNull { it.startTime }
                val summary = "${activeSlots.size} classes scheduled. First: ${firstClass?.subjectName} at ${firstClass?.startTime} (${firstClass?.roomNo})"

                NotificationHelper.dispatchMorningBriefing(
                    context = appContext,
                    title = "Today's Schedule",
                    body = summary
                )
            } finally {
                asyncPendingResult.finish()
            }
        }
    }
}
