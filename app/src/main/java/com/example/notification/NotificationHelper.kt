package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

object NotificationHelper {

    const val CHANNEL_LECTURES = "campus_lectures"
    const val CHANNEL_BRIEFING = "campus_briefing"
    const val CHANNEL_DEADLINES = "campus_deadlines"

    const val ACTION_MORNING_BRIEFING = "com.example.campustrack.ACTION_MORNING_BRIEFING"
    const val ACTION_PRE_LECTURE = "com.example.campustrack.ACTION_PRE_LECTURE"

    private const val MORNING_BRIEFING_REQUEST_CODE = 888
    private const val NOTIFICATION_ID_BRIEFING = 1001
    private const val NOTIFICATION_ID_HOLIDAY = 1002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val briefingChannel = NotificationChannel(
                CHANNEL_BRIEFING,
                "Daily Morning Briefing",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily 07:30 AM numbered InboxStyle summary of scheduled lectures and room numbers"
            }

            val lectureChannel = NotificationChannel(
                CHANNEL_LECTURES,
                "Pre-Lecture Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Exact notifications 10 minutes before period commencement"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(briefingChannel, lectureChannel)
            )
        }
    }

    /**
     * Multi-Line Numbered 07:30 AM Morning Briefing using NotificationCompat.InboxStyle()
     */
    fun showMorningBriefingInbox(
        context: Context,
        title: String,
        lines: List<String>,
        summaryText: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_BRIEFING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
        if (summaryText != null) {
            inboxStyle.setSummaryText(summaryText)
        }
        lines.forEach { line ->
            inboxStyle.addLine(line)
        }

        val collapsedText = if (lines.isNotEmpty()) lines.first() else "No classes scheduled today."

        val notification = NotificationCompat.Builder(context, CHANNEL_BRIEFING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(collapsedText)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BRIEFING, notification)
        } catch (_: SecurityException) {}
    }

    /**
     * Single Clean Holiday Notice when campus is closed.
     * Title: "Campus Closed • Holiday"
     * Text: "Today is [Holiday Name]. No classes scheduled."
     */
    fun showHolidayAlert(context: Context, title: String, bodyText: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_HOLIDAY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BRIEFING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_HOLIDAY, notification)
        } catch (_: SecurityException) {}
    }

    fun showMorningBriefing(context: Context, bodyText: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_BRIEFING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BRIEFING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Caliper Schedule")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BRIEFING, notification)
        } catch (_: SecurityException) {}
    }

    /**
     * High-Priority 10-Minute Pre-Lecture Heads-Up Alert
     * Includes subject name, exact start-end time, and room number.
     */
    fun showPreLectureHeadsUp(
        context: Context,
        subjectName: String,
        roomNo: String,
        startTime: String,
        endTime: String
    ) {
        val notificationId = ((subjectName.hashCode() xor startTime.hashCode()) and 0x7FFF) + 2000
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeSpan = if (endTime.isNotBlank()) "$startTime - $endTime" else startTime
        val bodyContent = "$subjectName • $timeSpan • Room: $roomNo"

        val notification = NotificationCompat.Builder(context, CHANNEL_LECTURES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Upcoming Lecture in 10 Minutes")
            .setContentText(bodyContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {}
    }

    fun showPreLectureAlert(context: Context, subjectName: String, roomNo: String, time: String) {
        showPreLectureHeadsUp(
            context = context,
            subjectName = subjectName,
            roomNo = roomNo,
            startTime = time,
            endTime = ""
        )
    }

    /**
     * Schedules the daily 07:30 AM briefing alarm directly into ScheduleAlarmReceiver
     * using exact alarms when permitted on Android 12+, with graceful fallback to setAndAllowWhileIdle().
     */
    fun scheduleDailyMorningBriefing(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_MORNING_BRIEFING
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MORNING_BRIEFING_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        scheduleRtcAlarmSafely(alarmManager, calendar.timeInMillis, pendingIntent)
    }

    /**
     * Schedules an exact 10-minute pre-lecture alarm for a specific slot occurring TODAY.
     * Fires at `startTime.minusMinutes(10)` only if still in the future.
     */
    fun scheduleExactPreLectureForToday(
        context: Context,
        slotId: Long,
        subjectName: String,
        roomNo: String,
        startTimeStr: String,
        endTimeStr: String = ""
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        try {
            val sTime = LocalTime.parse(startTimeStr)
            val triggerDateTime = LocalDateTime.of(LocalDate.now(), sTime).minusMinutes(10)
            val now = LocalDateTime.now()
            if (!triggerDateTime.isAfter(now)) return

            val triggerMillis = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val pendingIntent = buildPreLecturePendingIntent(
                context = context,
                slotId = slotId,
                subjectName = subjectName,
                roomNo = roomNo,
                startTimeStr = startTimeStr,
                endTimeStr = endTimeStr
            )
            scheduleRtcAlarmSafely(alarmManager, triggerMillis, pendingIntent)
        } catch (_: Exception) {}
    }

    /**
     * Dynamic Pre-Lecture Scheduling (weekly recurring lookup) with safe exact alarm check.
     * Fires precisely 10 minutes prior to slot.startTime.
     */
    fun schedulePreLectureAlert(
        context: Context,
        slotId: Long,
        subjectName: String,
        roomNo: String,
        startTimeStr: String,
        endTimeStr: String = "",
        dayOfWeek: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        try {
            val sTime = LocalTime.parse(startTimeStr)
            val today = LocalDate.now()
            val currentDayOfWeek = today.dayOfWeek.value

            var targetDate = today.plusDays(((dayOfWeek - currentDayOfWeek + 7) % 7).toLong())
            var targetDateTime = LocalDateTime.of(targetDate, sTime).minusMinutes(10)

            if (!targetDateTime.isAfter(LocalDateTime.now())) {
                targetDate = targetDate.plusDays(7)
                targetDateTime = LocalDateTime.of(targetDate, sTime).minusMinutes(10)
            }

            val triggerMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val pendingIntent = buildPreLecturePendingIntent(
                context = context,
                slotId = slotId,
                subjectName = subjectName,
                roomNo = roomNo,
                startTimeStr = startTimeStr,
                endTimeStr = endTimeStr
            )
            scheduleRtcAlarmSafely(alarmManager, triggerMillis, pendingIntent)
        } catch (_: Exception) {}
    }

    /**
     * Cancels a previously scheduled pre-lecture PendingIntent for the given slotId
     * (used during Holiday / Off-Day auto-mute suppression).
     */
    fun cancelPreLectureAlert(context: Context, slotId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PreLectureReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeForSlot(slotId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun buildPreLecturePendingIntent(
        context: Context,
        slotId: Long,
        subjectName: String,
        roomNo: String,
        startTimeStr: String,
        endTimeStr: String
    ): PendingIntent {
        val intent = Intent(context, PreLectureReceiver::class.java).apply {
            action = ACTION_PRE_LECTURE
            putExtra("slotId", slotId)
            putExtra("subjectName", subjectName)
            putExtra("roomNo", roomNo)
            putExtra("startTime", startTimeStr)
            putExtra("endTime", endTimeStr)
            putExtra("time", startTimeStr)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeForSlot(slotId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCodeForSlot(slotId: Long): Int {
        // Stable non-colliding request code for positive regular slot IDs and negative extra slot IDs
        return if (slotId >= 0L) {
            (slotId and 0x3FFFFFFF).toInt() + 10000
        } else {
            ((-slotId) and 0x3FFFFFFF).toInt() + 500000
        }
    }

    private fun scheduleRtcAlarmSafely(
        alarmManager: AlarmManager,
        triggerMillis: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            val canUseExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            if (canUseExact) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            }
        } catch (_: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        }
    }
}
