package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Dedicated PreLectureReceiver for firing 10-minute pre-lecture heads-up alerts.
 * Triggered by exact alarms scheduled via AlarmManager.setExactAndAllowWhileIdle().
 * Automatically mutes if today is a holiday or Sunday.
 */
class PreLectureReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val asyncPendingResult = goAsync()
        val appContext = context.applicationContext
        val subjectName = intent?.getStringExtra("subjectName") ?: "Upcoming Lecture"
        val roomNo = intent?.getStringExtra("roomNo") ?: "Assigned LT/Lab"
        val startTime = intent?.getStringExtra("startTime") ?: intent?.getStringExtra("time") ?: ""
        val endTime = intent?.getStringExtra("endTime") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(appContext)
                val today = LocalDate.now()
                val isSunday = (today.dayOfWeek.value == 7)
                val holiday = db.holidayDao().getHolidayForDate(today.toString())

                // Suppress if declared holiday or Sunday
                if (holiday != null || isSunday) {
                    return@launch
                }

                NotificationHelper.showPreLectureHeadsUp(
                    context = appContext,
                    subjectName = subjectName,
                    roomNo = roomNo,
                    startTime = startTime,
                    endTime = endTime
                )
            } finally {
                asyncPendingResult.finish()
            }
        }
    }
}
