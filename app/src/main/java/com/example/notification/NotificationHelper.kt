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
    const val ACTION_DEADLINE_ALERT = "com.example.campustrack.ACTION_DEADLINE_ALERT"

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

            val deadlineChannel = NotificationChannel(
                CHANNEL_DEADLINES,
                "Submission & Deadline Countdown",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts 24 hours and 2 hours before file or drawing sheet submission deadlines"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(briefingChannel, lectureChannel, deadlineChannel)
            )
        }
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

    fun postQuietNotice(context: Context, title: String, message: String) {
        showMorningBriefing(context, "$title • $message")
    }

    fun dispatchMorningBriefing(context: Context, title: String, body: String) {
        showMorningBriefing(context, "$title: $body")
    }

    fun showPreLectureAlert(context: Context, subjectName: String, roomNo: String, time: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = "Starts at $time in $roomNo. Tap to mark attendance or proxy."
        val notification = NotificationCompat.Builder(context, CHANNEL_LECTURES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Upcoming Class in 10 Mins: $subjectName")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify((subjectName.hashCode() and 0xFFFF) + 2000, notification)
        } catch (_: SecurityException) {}
    }

    fun sendNotification(context: Context, id: Int, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_LECTURES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {}
    }

    fun showDeadlineAlert(context: Context, title: String, timeLeft: String, isCritical: Boolean) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            3001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val header = if (isCritical) "🚨 URGENT DEADLINE (< 2 Hours)" else "⏳ Submission Reminder ($timeLeft left)"
        val notification = NotificationCompat.Builder(context, CHANNEL_DEADLINES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(header)
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title is due soon. Verify drawing sheets/workshop files."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify((title.hashCode() and 0xFFFF) + 3000, notification)
        } catch (_: SecurityException) {}
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

    fun schedulePreLectureAlert(
        context: Context,
        slotId: Long,
        subjectName: String,
        roomNo: String,
        startTimeStr: String,
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

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_PRE_LECTURE
                putExtra("subjectName", subjectName)
                putExtra("roomNo", roomNo)
                putExtra("time", startTimeStr)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                slotId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (_: Exception) {}
    }

    fun scheduleDeadlineReminders(
        context: Context,
        assessmentId: Long,
        title: String,
        dueDateStr: String,
        dueTimeStr: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        try {
            val dueTime = if (dueTimeStr.contains(":")) dueTimeStr else "$dueTimeStr:00"
            val targetDue = LocalDateTime.parse("${dueDateStr}T${dueTime}")

            val now = LocalDateTime.now()

            // 1. 24 hours prior
            val alert24h = targetDue.minusHours(24)
            if (alert24h.isAfter(now)) {
                val millis24 = alert24h.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val intent24 = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_DEADLINE_ALERT
                    putExtra("title", title)
                    putExtra("timeLeft", "24 Hours")
                    putExtra("isCritical", false)
                }
                val pi24 = PendingIntent.getBroadcast(
                    context,
                    (assessmentId * 10 + 1).toInt(),
                    intent24,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.set(AlarmManager.RTC_WAKEUP, millis24, pi24)
            }

            // 2. 2 hours prior
            val alert2h = targetDue.minusHours(2)
            if (alert2h.isAfter(now)) {
                val millis2 = alert2h.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val intent2 = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_DEADLINE_ALERT
                    putExtra("title", title)
                    putExtra("timeLeft", "2 Hours")
                    putExtra("isCritical", true)
                }
                val pi2 = PendingIntent.getBroadcast(
                    context,
                    (assessmentId * 10 + 2).toInt(),
                    intent2,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.set(AlarmManager.RTC_WAKEUP, millis2, pi2)
            }
        } catch (_: Exception) {}
    }
}
