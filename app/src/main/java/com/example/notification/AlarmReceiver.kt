package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * AlarmReceiver — Handles BOOT_COMPLETED rescheduling and delegates any legacy
 * briefing or pre-lecture broadcast intents into ScheduleAlarmReceiver / PreLectureReceiver.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            NotificationHelper.ACTION_MORNING_BRIEFING -> {
                val forwardIntent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
                    this.action = NotificationHelper.ACTION_MORNING_BRIEFING
                }
                context.sendBroadcast(forwardIntent)
            }

            NotificationHelper.ACTION_PRE_LECTURE -> {
                val subjectName = intent.getStringExtra("subjectName") ?: "Upcoming Lecture"
                val roomNo = intent.getStringExtra("roomNo") ?: "Room"
                val startTime = intent.getStringExtra("startTime") ?: intent.getStringExtra("time") ?: "Soon"
                val endTime = intent.getStringExtra("endTime") ?: ""
                NotificationHelper.showPreLectureHeadsUp(context, subjectName, roomNo, startTime, endTime)
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                NotificationHelper.createNotificationChannels(context)
                NotificationHelper.scheduleDailyMorningBriefing(context)
            }
        }
    }
}
