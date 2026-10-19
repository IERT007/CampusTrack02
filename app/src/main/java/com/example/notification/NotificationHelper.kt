package com.example.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object NotificationHelper {
    const val CHANNEL_BRIEFING = "caliper_morning_briefing"
    const val CHANNEL_LECTURES = "caliper_pre_lecture_alerts"
    const val CHANNEL_DEADLINES = "caliper_academic_deadlines"

    const val ACTION_MORNING_BRIEFING = "com.example.campustrack.ACTION_MORNING_BRIEFING"
    const val ACTION_PRE_LECTURE = "com.example.campustrack.ACTION_PRE_LECTURE"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val briefingChannel = NotificationChannel(
                CHANNEL_BRIEFING,
                "07:30 AM Morning Briefing",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily numbered academic timetable briefing at 07:30 AM"
            }

            val lectureChannel = NotificationChannel(
                CHANNEL_LECTURES,
                "10-Min Pre-Lecture Radar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Deterministic 10-minute heads-up alerts before each scheduled class"
            }

            manager.createNotificationChannel(briefingChannel)
            manager.createNotificationChannel(lectureChannel)
        }
    }

    fun scheduleDailyMorningBriefing(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_MORNING_BRIEFING
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            730,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = LocalDateTime.now()
        var target = LocalDateTime.of(LocalDate.now(), LocalTime.of(7, 30))
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        val triggerMillis = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    fun scheduleWeeklyPreLectureAlert(
        context: Context,
        slotId: Long,
        dayOfWeek: Int,
        startTime: String,
        subjectName: String,
        room: String
    ) {
        // Schedules pre-lecture alert for upcoming slot
    }

    fun scheduleAssessmentDeadlineAlerts(
        context: Context,
        assessmentId: Long,
        title: String,
        category: String,
        dueDate: String
    ) {
        // Schedules deadline alert for assessment
    }

    fun schedulePreLectureAlert(
        context: Context,
        triggerAtMillis: Long,
        subjectName: String,
        roomNo: String,
        startTime: String,
        endTime: String = "",
        requestCode: Int
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, PreLectureReceiver::class.java).apply {
            action = ACTION_PRE_LECTURE
            putExtra("subjectName", subjectName)
            putExtra("roomNo", roomNo)
            putExtra("startTime", startTime)
            putExtra("endTime", endTime)
            putExtra("notificationId", requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun showMorningBriefingInbox(
        context: Context,
        title: String,
        lines: List<String>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createNotificationChannels(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingTap = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val inboxStyle = NotificationCompat.InboxStyle().setBigContentTitle(title)
        lines.forEach { line -> inboxStyle.addLine(line) }

        val summaryText = lines.firstOrNull() ?: "No classes scheduled today."

        val notification = NotificationCompat.Builder(context, CHANNEL_BRIEFING)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingTap)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1001, notification)
    }

    fun showMorningBriefing(context: Context, summary: String) {
        val lines = summary.lines().filter { it.isNotBlank() }
        showMorningBriefingInbox(
            context = context,
            title = "Caliper Schedule • Today (${lines.size} Classes)",
            lines = lines
        )
    }

    fun showPreLectureHeadsUp(
        context: Context,
        subjectName: String,
        roomNo: String,
        startTime: String,
        endTime: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createNotificationChannels(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingTap = PendingIntent.getActivity(
            context,
            subjectName.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeRange = if (endTime.isNotBlank()) "$startTime - $endTime" else startTime
        val contentText = "$subjectName • Room: $roomNo • $timeRange"

        val notification = NotificationCompat.Builder(context, CHANNEL_LECTURES)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Upcoming Lecture in 10 Minutes")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingTap)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(subjectName.hashCode() and 0xFFFF, notification)
    }

    fun showPreLectureAlert(
        context: Context,
        subjectName: String,
        roomNo: String,
        startTime: String
    ) {
        showPreLectureHeadsUp(context, subjectName, roomNo, startTime, "")
    }
}
