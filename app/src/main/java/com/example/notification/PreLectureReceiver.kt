package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Dedicated PreLectureReceiver for firing 10-minute pre-lecture heads-up alerts.
 * Triggered by exact alarms scheduled via AlarmManager.setExactAndAllowWhileIdle().
 * Issues a high-priority Heads-Up notification displaying:
 * - Title: Upcoming Lecture in 10 Minutes
 * - Content: [Subject Name] • Room: [RoomNo] • [StartTime] - [EndTime]
 */
class PreLectureReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val subjectName = intent?.getStringExtra("subjectName") ?: "Upcoming Lecture"
        val roomNo = intent?.getStringExtra("roomNo") ?: "Assigned LT/Lab"
        val startTime = intent?.getStringExtra("startTime") ?: intent?.getStringExtra("time") ?: ""
        val endTime = intent?.getStringExtra("endTime") ?: ""

        NotificationHelper.showPreLectureHeadsUp(
            context = context.applicationContext,
            subjectName = subjectName,
            roomNo = roomNo,
            startTime = startTime,
            endTime = endTime
        )
    }
}
