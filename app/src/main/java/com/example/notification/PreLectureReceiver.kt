package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Dedicated PreLectureReceiver for firing 10-minute pre-lecture heads-up alerts.
 * Triggered by exact alarms scheduled via AlarmManager.setExactAndAllowWhileIdle().
 */
class PreLectureReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val subjectName = intent?.getStringExtra("subjectName") ?: "Upcoming Lecture"
        val roomNo = intent?.getStringExtra("roomNo") ?: "Assigned LT/Lab"
        val time = intent?.getStringExtra("startTime") ?: intent?.getStringExtra("time") ?: "in 10 mins"

        NotificationHelper.showPreLectureAlert(
            context = context.applicationContext,
            subjectName = subjectName,
            roomNo = roomNo,
            time = time
        )
    }
}
