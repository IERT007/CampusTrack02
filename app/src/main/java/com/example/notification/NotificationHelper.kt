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

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val briefingChannel = NotificationChannel(
                CHANNEL_BRIEFING,
                "Daily Morning Briefing",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily 07:30 AM summary of scheduled lectures and room numbers"
            }

            val lectureChannel = NotificationChannel(
                CHANNEL_LECTURES,
                "Pre-Lecture Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications 10 minutes before period commencement"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(briefingChannel, lectureChannel)
            )
        }
    }

    /**
     * Numbered 07:30 AM Morning Briefing using NotificationCompat.InboxStyle
     * Formats strictly:
     * Title: Caliper Schedule • Today (N Classes)
     * Lines: 1. [StartTime] — [Subject Name] (Room [RoomNo])
     */
    fun showMorningBriefingInbox(context: Context, title: String, lines: List<String>) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val inboxStyle = NotificationCompat.InboxStyle().setBigContentTitle(title)
        lines.forEach { line ->
            inboxStyle.addLine(line)
        }

        val summaryText = if (lines.isNotEmpty()) lines.first() else "No scheduled lectures today"

        val notification = NotificationCompat.Builder(context, CHANNEL_BRIEFING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(1001, notification)
        } catch (_: SecurityException) {}
    }

    fun showMorningBriefing(context: Context, bodyText: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BRIEFING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Caliper: Today's Schedule Briefing")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(1001, notification)
        } catch (_: SecurityException) {}
    }

    /**
     * High-Priority Heads-Up Pre-Lecture Alert
     * Title: Upcoming Lecture in 10 Minutes
     * Content: [Subject Name] • Room: [RoomNo] • [StartTime] - [EndTime]
     */
    fun showPreLectureHeadsUp(
        context: Context,
        subjectName: String,
        roomNo: String,
        startTime: String,
        endTime: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (subjectName.hashCode() and 0xFFFF) + 2000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeSpan = if (endTime.isNotBlank()) "$startTime - $endTime" else startTime
        val bodyContent = "$subjectName • Room: $roomNo • $timeSpan"

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
            NotificationManagerCompat.from(context).notify(
                (subjectName.hashCode() and 0xFFFF) + 2000,
                notification
            )
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

    fun scheduleDailyMorningBriefing(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_MORNING_BRIEFING
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            888,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DATE, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    /**
     * Dynamic Pre-Lecture Scheduling with AlarmManager.setExactAndAllowWhileIdle()
     * Fires precisely 10 minutes prior to slot.startTime
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

            if (targetDateTime.isBefore(LocalDateTime.now())) {
                targetDate = targetDate.plusDays(7)
                targetDateTime = LocalDateTime.of(targetDate, sTime).minusMinutes(10)
            }

            val triggerMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val intent = Intent(context, PreLectureReceiver::class.java).apply {
                putExtra("subjectName", subjectName)
                putExtra("roomNo", roomNo)
                putExtra("startTime", startTimeStr)
                putExtra("endTime", endTimeStr)
                putExtra("time", startTimeStr)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (slotId and 0x7FFFFFFF).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (_: Exception) {}
    }
}
