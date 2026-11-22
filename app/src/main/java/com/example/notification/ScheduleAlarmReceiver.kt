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
import java.time.format.DateTimeFormatter

/**
 * ScheduleAlarmReceiver — Daily 07:30 AM Morning Briefing & Pre-Lecture Alarm Dispatcher.
 *
 * Inside goAsync():
 * 1. Checks HolidayRangeDao, DailyDayStatusDao, Sunday, and active Semester date bounds FIRST.
 * 2. On declared holidays/off-days: cancels/suppresses all 10-min pre-lecture alarms and sends
 *    a single clean "Campus Closed • Holiday" notice.
 * 3. On active class days: builds a multi-line numbered InboxStyle briefing (merging regular
 *    slots, RescheduleOverrideEntity, and ExtraSlotEntity) and schedules exact 10-minute
 *    pre-lecture heads-up alarms for every upcoming lecture.
 */
class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val asyncPendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationHelper.createNotificationChannels(appContext)

                val db = AppDatabase.getInstance(appContext)
                val today = LocalDate.now()
                val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val dayOfWeek = today.dayOfWeek.value // 1 = Monday, 7 = Sunday
                val nowTime = LocalTime.now()

                val prefs = appContext.getSharedPreferences("caliper_settings", Context.MODE_PRIVATE)
                val semesterStartDate = prefs.getString("semester_start_date", "2026-07-15") ?: "2026-07-15"
                val semesterEndDate = prefs.getString("semester_end_date", "2026-12-15") ?: "2026-12-15"

                val holiday = db.holidayRangeDao().getHolidayForDate(todayStr)
                val dayStatus = db.dailyDayStatusDao().getDayStatusDirect(todayStr)
                val isDeclaredOffDay = dayStatus?.leaveCategory in listOf("college_holiday", "strike")
                val isSunday = (dayOfWeek == 7)
                val isOutsideSemester = (todayStr < semesterStartDate || todayStr > semesterEndDate)

                val baseSlots = db.timetableSlotDao().getSlotsForDayDirect(dayOfWeek)
                val extraSlots = db.extraSlotDao().getExtraSlotsForDateDirect(todayStr)

                // Holiday & Off-Day Auto-Mute
                if (holiday != null || isDeclaredOffDay || isSunday || isOutsideSemester) {
                    // Actively suppress/cancel all 10-minute pre-lecture alarms for today
                    baseSlots.forEach { slot ->
                        NotificationHelper.cancelPreLectureAlert(appContext, slot.id)
                    }
                    extraSlots.forEach { extra ->
                        NotificationHelper.cancelPreLectureAlert(appContext, -extra.id)
                    }

                    // Dispatch a single clean holiday notice if declared holiday or institutional closure
                    if (holiday != null || isDeclaredOffDay) {
                        val holidayName = holiday?.title
                            ?: dayStatus?.notes?.takeIf { it.isNotBlank() }
                            ?: "Campus Holiday"
                        NotificationHelper.showHolidayAlert(
                            context = appContext,
                            title = "Campus Closed • Holiday",
                            bodyText = "Today is $holidayName. No classes scheduled."
                        )
                    }

                    NotificationHelper.scheduleDailyMorningBriefing(appContext)
                    return@launch
                }

                // Resolve active classes for today (regular + reschedule overrides + extra slots)
                val overrides = db.rescheduleOverrideDao().getOverridesForDateDirect(todayStr)

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
                        roomNo = extra.roomNo,
                        subjectId = extra.subjectId
                    )
                }

                val allTodaySlots = (regularSlots + extraAsSlots).sortedBy { it.startTime }

                if (allTodaySlots.isEmpty()) {
                    NotificationHelper.scheduleDailyMorningBriefing(appContext)
                    return@launch
                }

                val subjects = db.subjectDao().getAllSubjectsList()

                // Multi-Line Numbered Morning Briefing using NotificationCompat.InboxStyle()
                val numberedLines = allTodaySlots.mapIndexed { index, slot ->
                    val sub = subjects.find { it.id == slot.subjectId }
                    val subjectName = sub?.name ?: sub?.code ?: "Lecture"
                    "${index + 1}. ${slot.startTime} - ${slot.endTime} | $subjectName (${slot.roomNo})"
                }

                val briefingTitle = "Caliper Schedule • ${allTodaySlots.size} Classes Today"

                NotificationHelper.showMorningBriefingInbox(
                    context = appContext,
                    title = briefingTitle,
                    lines = numberedLines,
                    summaryText = "${allTodaySlots.size} classes scheduled today"
                )

                // Schedule exact 10-minute pre-lecture heads-up alarms for every remaining class today
                allTodaySlots.forEach { slot ->
                    val isUpcoming = try {
                        LocalTime.parse(slot.startTime).minusMinutes(10).isAfter(nowTime)
                    } catch (_: Exception) {
                        false
                    }

                    if (isUpcoming) {
                        val sub = subjects.find { it.id == slot.subjectId }
                        val subjectName = sub?.name ?: sub?.code ?: "Lecture"
                        NotificationHelper.scheduleExactPreLectureForToday(
                            context = appContext,
                            slotId = slot.id,
                            subjectName = subjectName,
                            roomNo = slot.roomNo,
                            startTimeStr = slot.startTime,
                            endTimeStr = slot.endTime
                        )
                    }
                }

                // Reschedule tomorrow's 07:30 AM briefing
                NotificationHelper.scheduleDailyMorningBriefing(appContext)
            } finally {
                asyncPendingResult.finish()
            }
        }
    }
}
