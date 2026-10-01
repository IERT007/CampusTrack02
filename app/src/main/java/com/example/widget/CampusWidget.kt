package com.example.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AttendanceLogEntity
import com.example.data.repository.CampusRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CampusWidget : GlanceAppWidget() {

    companion object {
        val PARAM_SLOT_ID = ActionParameters.Key<Long>("slot_id")
        val PARAM_SUBJECT_ID = ActionParameters.Key<Long>("subject_id")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dayOfWeek = today.dayOfWeek.value // 1 = Monday ... 7 = Sunday
        val formattedDate = today.format(DateTimeFormatter.ofPattern("EEE, dd MMM"))

        val subjects = db.subjectDao().getAllSubjects().first()
        val allSlots = db.timetableSlotDao().getAllSlots().first().filter { it.dayOfWeek == dayOfWeek }.sortedBy { it.startTime }
        val logs = db.attendanceLogDao().getLogsForDate(todayStr).first()
        val allLogs = db.attendanceLogDao().getAllLogs().first()

        // Calculate Safe-zone %
        var totalAtt = 0
        var totalCond = 0
        for (sub in subjects) {
            val subLogs = allLogs.filter { it.subjectId == sub.id }
            val att = subLogs.count { it.status == "attended" } + sub.reconciledAttendedOffset
            val bunk = subLogs.count { it.status == "bunked" }
            val cond = att + bunk + sub.reconciledTotalOffset
            totalAtt += kotlin.math.max(0, att)
            totalCond += kotlin.math.max(0, cond)
        }
        val safeZonePct = if (totalCond > 0) (totalAtt.toDouble() / totalCond.toDouble()) * 100.0 else 100.0

        // Find active or next upcoming slot
        val upcomingSlot = allSlots.firstOrNull { slot ->
            val log = logs.find { it.slotId == slot.id }
            log == null // Unmarked period
        } ?: allSlots.firstOrNull()

        val activeSubject = subjects.find { it.id == upcomingSlot?.subjectId }
        val isMarked = logs.any { it.slotId == upcomingSlot?.id }

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(18.dp)
                    .background(Color(0xFF05070B))
                    .padding(14.dp)
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    // Header: App Title, Date & Safe Zone %
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = "CALIPER • IERT",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF38BDF8)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = formattedDate,
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF94A3B8)),
                                    fontSize = 10.sp
                                )
                            )
                        }

                        // Live Safe-Zone % badge
                        val pctColor = if (safeZonePct >= 75.0) Color(0xFF34D399) else Color(0xFFFB7185)
                        Box(
                            modifier = GlanceModifier
                                .cornerRadius(8.dp)
                                .background(Color(0x1A38BDF8))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${String.format("%.1f", safeZonePct)}% Safe",
                                style = TextStyle(
                                    color = ColorProvider(pctColor),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    // Lecture Info
                    if (upcomingSlot != null && activeSubject != null) {
                        Text(
                            text = activeSubject.name,
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFF8FAFC)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = "${activeSubject.code} • ${upcomingSlot.startTime} - ${upcomingSlot.endTime} • ${upcomingSlot.roomNo}",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF38BDF8)),
                                fontSize = 11.sp
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(10.dp))

                        // Direct Action Buttons: 1-Tap Present & Bunk
                        if (!isMarked) {
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    text = "✓ Present",
                                    onClick = actionRunCallback<MarkPresentActionCallback>(
                                        actionParametersOf(
                                            PARAM_SLOT_ID to upcomingSlot.id,
                                            PARAM_SUBJECT_ID to activeSubject.id
                                        )
                                    ),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = ColorProvider(Color(0xFF34D399)),
                                        contentColor = ColorProvider(Color(0xFF05070B))
                                    ),
                                    modifier = GlanceModifier.defaultWeight().height(36.dp)
                                )

                                Spacer(modifier = GlanceModifier.width(8.dp))

                                Button(
                                    text = "✕ Bunk",
                                    onClick = actionRunCallback<MarkBunkActionCallback>(
                                        actionParametersOf(
                                            PARAM_SLOT_ID to upcomingSlot.id,
                                            PARAM_SUBJECT_ID to activeSubject.id
                                        )
                                    ),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = ColorProvider(Color(0xFFFB7185)),
                                        contentColor = ColorProvider(Color.White)
                                    ),
                                    modifier = GlanceModifier.defaultWeight().height(36.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "✓ Period Attendance Logged",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF34D399)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else {
                        Text(
                            text = "No further classes scheduled for today.",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF94A3B8)),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

class CampusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CampusWidget()
}

class MarkPresentActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val slotId = parameters[CampusWidget.PARAM_SLOT_ID] ?: return
        val subjectId = parameters[CampusWidget.PARAM_SUBJECT_ID] ?: return
        val db = AppDatabase.getDatabase(context)
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        db.attendanceLogDao().deleteLogForSlot(todayStr, slotId)
        db.attendanceLogDao().insertLog(
            AttendanceLogEntity(
                date = todayStr,
                slotId = slotId,
                subjectId = subjectId,
                status = "attended",
                isProxy = false,
                isExtraClass = false,
                notes = "Marked via Caliper Widget",
                timestamp = System.currentTimeMillis()
            )
        )

        CampusRepository(context, db).triggerSilentAutoBackup()
        CampusWidget().update(context, glanceId)
    }
}

class MarkBunkActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val slotId = parameters[CampusWidget.PARAM_SLOT_ID] ?: return
        val subjectId = parameters[CampusWidget.PARAM_SUBJECT_ID] ?: return
        val db = AppDatabase.getDatabase(context)
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        db.attendanceLogDao().deleteLogForSlot(todayStr, slotId)
        db.attendanceLogDao().insertLog(
            AttendanceLogEntity(
                date = todayStr,
                slotId = slotId,
                subjectId = subjectId,
                status = "bunked",
                isProxy = false,
                isExtraClass = false,
                notes = "Bunked via Caliper Widget",
                timestamp = System.currentTimeMillis()
            )
        )

        CampusRepository(context, db).triggerSilentAutoBackup()
        CampusWidget().update(context, glanceId)
    }
}
