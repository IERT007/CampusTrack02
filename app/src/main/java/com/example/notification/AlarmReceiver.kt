package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                NotificationHelper.createNotificationChannels(context)
                NotificationHelper.scheduleDailyMorningBriefing(context)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val today = LocalDate.now()
                        val nowTime = LocalTime.now()
                        val slots = db.timetableSlotDao().getSlotsForDayDirect(today.dayOfWeek.value)
                        val subjectsMap = db.subjectDao().getAllSubjectsList().associateBy { it.id }

                        slots.forEach { slot ->
                            val start = runCatching { LocalTime.parse(slot.startTime) }.getOrNull()
                            if (start != null && start.isAfter(nowTime)) {
                                val triggerMillis = LocalDateTime.of(today, start)
                                    .minusMinutes(10)
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()
                                val subjectName = subjectsMap[slot.subjectId]?.name ?: "Lecture"
                                NotificationHelper.schedulePreLectureAlert(
                                    context = context,
                                    triggerAtMillis = triggerMillis,
                                    subjectName = subjectName,
                                    roomNo = slot.roomNo,
                                    startTime = slot.startTime,
                                    endTime = slot.endTime,
                                    requestCode = (slot.id.toInt() + 2000)
                                )
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            NotificationHelper.ACTION_MORNING_BRIEFING -> {
                ScheduleAlarmReceiver().onReceive(context, intent)
            }
            NotificationHelper.ACTION_PRE_LECTURE -> {
                PreLectureReceiver().onReceive(context, intent)
            }
        }
    }
}
