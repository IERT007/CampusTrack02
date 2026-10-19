package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PreLectureReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val subjectName = intent?.getStringExtra("subjectName") ?: "Scheduled Lecture"
        val roomNo = intent?.getStringExtra("roomNo") ?: "N/A"
        val startTime = intent?.getStringExtra("startTime") ?: ""
        val endTime = intent?.getStringExtra("endTime") ?: ""

        NotificationHelper.showPreLectureHeadsUp(
            context = context,
            subjectName = subjectName,
            roomNo = roomNo,
            startTime = startTime,
            endTime = endTime
        )
    }
}
