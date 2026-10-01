package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            NotificationHelper.ACTION_MORNING_BRIEFING -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val today = LocalDate.now()
                        val dayOfWeek = today.dayOfWeek.value // 1 = Monday ... 7 = Sunday
                        val slots = db.timetableSlotDao().getAllSlots().first().filter { it.dayOfWeek == dayOfWeek }
                        val subjects = db.subjectDao().getAllSubjects().first()

                        val summary = if (slots.isEmpty()) {
                            "No classes scheduled today! Enjoy your holiday or catch up on drawing sheets."
                        } else {
                            val items = slots.take(4).map { slot ->
                                val sub = subjects.find { it.id == slot.subjectId }
                                "${sub?.code ?: "Lecture"} (${slot.startTime} in ${slot.roomNo})"
                            }.joinToString(", ")
                            "${slots.size} Periods scheduled: $items${if (slots.size > 4) "..." else ""}"
                        }

                        NotificationHelper.showMorningBriefing(context, summary)
                        // Schedule for tomorrow
                        NotificationHelper.scheduleDailyMorningBriefing(context)
                    } catch (_: Exception) {}
                }
            }

            NotificationHelper.ACTION_PRE_LECTURE -> {
                val subjectName = intent.getStringExtra("subjectName") ?: "Upcoming Lecture"
                val roomNo = intent.getStringExtra("roomNo") ?: "Room"
                val time = intent.getStringExtra("time") ?: "Soon"
                NotificationHelper.showPreLectureAlert(context, subjectName, roomNo, time)
            }

            NotificationHelper.ACTION_DEADLINE_ALERT -> {
                val title = intent.getStringExtra("title") ?: "Submission Due"
                val timeLeft = intent.getStringExtra("timeLeft") ?: "Few Hours"
                val isCritical = intent.getBooleanExtra("isCritical", false)
                NotificationHelper.showDeadlineAlert(context, title, timeLeft, isCritical)
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                NotificationHelper.createNotificationChannels(context)
                NotificationHelper.scheduleDailyMorningBriefing(context)
            }
        }
    }
}
