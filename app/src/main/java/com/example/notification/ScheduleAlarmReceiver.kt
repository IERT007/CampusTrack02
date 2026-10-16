package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val asyncPendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(appContext)
                val today = LocalDate.now()
                val dayOfWeek = today.dayOfWeek.value // 1 = Monday, 7 = Sunday
                val nowTime = LocalTime.now()

                // Check Institutional Closures and Holidays
                val holiday = db.holidayDao().getHolidayForDate(today.toString())
                if (holiday != null || dayOfWeek == 7) {
                    NotificationHelper.showMorningBriefing(
                        context = appContext,
                        bodyText = "Campus Closed • ${holiday?.title ?: "Sunday — No academic periods scheduled."}"
                    )
                    return@launch
                }

                // Query Active Scheduled Classes
                val activeSlots = db.timetableSlotDao().getSlotsForDayDirect(dayOfWeek)
                if (activeSlots.isEmpty()) {
                    NotificationHelper.showMorningBriefing(
                        context = appContext,
                        bodyText = "Caliper Schedule • Today (0 Classes)\nNo timetable lectures scheduled for today."
                    )
                    return@launch
                }

                val subjects = db.subjectDao().getAllSubjectsList()
                val sortedSlots = activeSlots.sortedBy { it.startTime }

                val lines = sortedSlots.mapIndexed { index, slot ->
                    val sub = subjects.find { it.id == slot.subjectId }
                    val subjectName = sub?.name ?: sub?.code ?: "Lecture"
                    "${index + 1}. [${slot.startTime}] — [$subjectName] (Room [${slot.roomNo}])"
                }

                val briefingTitle = "Caliper Schedule • Today (${sortedSlots.size} Classes)"

                // Numbered 07:30 AM Morning Briefing using NotificationCompat.InboxStyle()
                NotificationHelper.showMorningBriefingInbox(
                    context = appContext,
                    title = briefingTitle,
                    lines = lines
                )

                // Deterministic 10-Minute Pre-Lecture Heads-Up:
                // Query active upcoming slots from Room DB where slot.startTime > LocalTime.now()
                val upcomingSlots = sortedSlots.filter { slot ->
                    try {
                        val sTime = LocalTime.parse(slot.startTime)
                        sTime.isAfter(nowTime)
                    } catch (_: Exception) {
                        true
                    }
                }

                // Schedule an exact alarm firing precisely 10 minutes before each upcoming slot's start time
                upcomingSlots.forEach { slot ->
                    val sub = subjects.find { it.id == slot.subjectId }
                    val subjectName = sub?.name ?: sub?.code ?: "Lecture"
                    NotificationHelper.schedulePreLectureAlert(
                        context = appContext,
                        slotId = slot.id,
                        subjectName = subjectName,
                        roomNo = slot.roomNo,
                        startTimeStr = slot.startTime,
                        endTimeStr = slot.endTime,
                        dayOfWeek = dayOfWeek
                    )
                }

                // Reschedule for next day 07:30 AM
                NotificationHelper.scheduleDailyMorningBriefing(appContext)
            } finally {
                asyncPendingResult.finish()
            }
        }
    }
}
