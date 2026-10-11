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
                val briefingBody = "$briefingTitle\n" + lines.joinToString("\n")

                NotificationHelper.showMorningBriefing(
                    context = appContext,
                    bodyText = briefingBody
                )

                // Schedule exact 10-Minute Pre-Lecture Heads-Up alerts for each slot today
                sortedSlots.forEach { slot ->
                    val sub = subjects.find { it.id == slot.subjectId }
                    val subjectName = sub?.name ?: sub?.code ?: "Lecture"
                    NotificationHelper.schedulePreLectureAlert(
                        context = appContext,
                        slotId = slot.id,
                        subjectName = subjectName,
                        roomNo = slot.roomNo,
                        startTimeStr = slot.startTime,
                        dayOfWeek = dayOfWeek
                    )
                }

                // Reschedule for next day
                NotificationHelper.scheduleDailyMorningBriefing(appContext)
            } finally {
                asyncPendingResult.finish()
            }
        }
    }
}
