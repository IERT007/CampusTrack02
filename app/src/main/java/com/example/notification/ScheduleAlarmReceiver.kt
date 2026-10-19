package com.example.notification

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationHelper.createNotificationChannels(context)
                val db = AppDatabase.getDatabase(context)
                val today = LocalDate.now()
                val todayStr = today.toString()

                val holiday = db.holidayRangeDao().getHolidayForDate(todayStr)
                if (holiday == null && today.dayOfWeek.value <= 6) {
                    val slots = db.timetableSlotDao()
                        .getSlotsForDayDirect(today.dayOfWeek.value)
                        .sortedBy { it.startTime }

                    if (slots.isNotEmpty()) {
                        val subjectsMap = db.subjectDao()
                            .getAllSubjectsList()
                            .associateBy { it.id }

                        val formattedLines = slots.mapIndexed { index, slot ->
                            val subjectName = subjectsMap[slot.subjectId]?.name ?: "Lecture"
                            "${index + 1}. ${slot.startTime} — $subjectName (Room ${slot.roomNo})"
                        }

                        val title = "Caliper Schedule • Today (${slots.size} Classes)"
                        val inboxStyle = NotificationCompat.InboxStyle()
                            .setBigContentTitle(title)
                        formattedLines.forEach { line ->
                            inboxStyle.addLine(line)
                        }

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            val tapIntent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            val pendingTap = PendingIntent.getActivity(
                                context,
                                730,
                                tapIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )

                            val notification = NotificationCompat.Builder(
                                context,
                                NotificationHelper.CHANNEL_BRIEFING
                            )
                                .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                                .setContentTitle(title)
                                .setContentText(formattedLines.first())
                                .setStyle(inboxStyle)
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setContentIntent(pendingTap)
                                .setAutoCancel(true)
                                .build()

                            NotificationManagerCompat.from(context).notify(1001, notification)
                        }

                        // Deterministically schedule today's 10-minute pre-lecture heads-up alarms
                        val nowTime = LocalTime.now()
                        slots.forEach { slot ->
                            val slotStart = runCatching { LocalTime.parse(slot.startTime) }.getOrNull()
                            if (slotStart != null && slotStart.isAfter(nowTime)) {
                                val triggerDateTime = LocalDateTime.of(today, slotStart).minusMinutes(10)
                                val triggerMillis = triggerDateTime
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
                    }
                }

                // Re-arm for tomorrow 07:30 AM
                NotificationHelper.scheduleDailyMorningBriefing(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
