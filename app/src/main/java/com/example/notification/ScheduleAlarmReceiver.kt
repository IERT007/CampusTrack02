package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.local.entity.TimetableSlotEntity
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
                val todayStr = today.toString()
                val dayOfWeek = today.dayOfWeek.value // 1 = Monday, 7 = Sunday
                val nowTime = LocalTime.now()

                // Section 5: Holiday Auto-Mute & Notification Suppression
                // Query HolidayDao for today's date
                val holiday = db.holidayDao().getHolidayForDate(todayStr)
                if (holiday != null || dayOfWeek == 7) {
                    val holidayName = holiday?.title ?: if (dayOfWeek == 7) "Sunday Break" else "Institutional Closure"
                    
                    // 1. Suppress all pre-lecture notification alarms for the day
                    // 2. Post dedicated Gazetted Holiday alert
                    NotificationHelper.showHolidayAlert(
                        context = appContext,
                        title = "Campus Closed • Gazetted Holiday",
                        bodyText = "Today is an official holiday ($holidayName). Enjoy your break!"
                    )

                    // Reschedule for next day 07:30 AM
                    NotificationHelper.scheduleDailyMorningBriefing(appContext)
                    return@launch
                }

                // Query Active Scheduled Classes
                val baseSlots = db.timetableSlotDao().getSlotsForDayDirect(dayOfWeek)
                val overrides = db.rescheduleOverrideDao().getOverridesForDateDirect(todayStr)
                val extraSlots = db.extraSlotDao().getExtraSlotsForDateDirect(todayStr)

                val regularSlots = baseSlots.map { slot ->
                    val override = overrides.find { it.slotId == slot.id }
                    if (override != null) {
                        slot.copy(
                            startTime = override.newStartTime,
                            endTime = override.newEndTime,
                            roomNo = override.newRoomNo ?: slot.roomNo
                        )
                    } else {
                        slot
                    }
                }

                val extraAsSlots = extraSlots.map { extra ->
                    TimetableSlotEntity(
                        id = -extra.id,
                        dayOfWeek = dayOfWeek,
                        startTime = extra.startTime,
                        endTime = extra.endTime,
                        roomNo = "${extra.roomNo} • ${extra.tag}",
                        subjectId = extra.subjectId
                    )
                }

                val allTodaySlots = (regularSlots + extraAsSlots).sortedBy { it.startTime }

                if (allTodaySlots.isEmpty()) {
                    NotificationHelper.showMorningBriefing(
                        context = appContext,
                        bodyText = "Today's Schedule • 0 Lectures\nNo timetable lectures scheduled for today."
                    )
                    NotificationHelper.scheduleDailyMorningBriefing(appContext)
                    return@launch
                }

                val subjects = db.subjectDao().getAllSubjectsList()

                // Section 6: Multi-line Bullet Schedule Notifications using NotificationCompat.InboxStyle()
                val lines = allTodaySlots.map { slot ->
                    val sub = subjects.find { it.id == slot.subjectId }
                    val subjectName = sub?.name ?: sub?.code ?: "Lecture"
                    "• ${slot.startTime} - ${slot.endTime} | $subjectName (${slot.roomNo})"
                }

                val briefingTitle = "Today's Schedule • ${allTodaySlots.size} Lectures"

                NotificationHelper.showMorningBriefingInbox(
                    context = appContext,
                    title = briefingTitle,
                    lines = lines,
                    summaryText = "Today's Schedule • ${allTodaySlots.size} Lectures"
                )

                // Deterministic 10-Minute Pre-Lecture Heads-Up for upcoming slots
                val upcomingSlots = allTodaySlots.filter { slot ->
                    try {
                        val sTime = LocalTime.parse(slot.startTime)
                        sTime.isAfter(nowTime)
                    } catch (_: Exception) {
                        true
                    }
                }

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
