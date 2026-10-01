package com.example.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CaliperMatrixWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val today = LocalDate.now()
        val allLogs = db.attendanceLogDao().getAllLogs().first()
        val allSubjects = db.subjectDao().getAllSubjects().first()

        var totalAtt = 0
        var totalCond = 0
        for (sub in allSubjects) {
            val subLogs = allLogs.filter { it.subjectId == sub.id }
            val att = subLogs.count { it.status == "attended" } + sub.reconciledAttendedOffset
            val bunk = subLogs.count { it.status == "bunked" }
            val cond = att + bunk + sub.reconciledTotalOffset
            totalAtt += kotlin.math.max(0, att)
            totalCond += kotlin.math.max(0, cond)
        }
        val safeZonePct = if (totalCond > 0) (totalAtt.toDouble() / totalCond.toDouble()) * 100.0 else 100.0

        // Look up last 14 days attendance status
        val dayStatuses = mutableListOf<Triple<String, Int, String>>() // (dayNumber, statusColorInt, label)
        for (i in 13 downTo 0) {
            val d = today.minusDays(i.toLong())
            val dStr = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val dLogs = allLogs.filter { it.date == dStr }

            val statusColor = when {
                dLogs.isEmpty() -> 0x3394A3B8 // Slate dim
                dLogs.any { it.status == "bunked" } -> 0xFFFB7185.toInt() // Coral Bunk
                dLogs.all { it.status == "cancelled_by_faculty" || it.status == "college_off" } -> 0xFFFBBF24.toInt() // Amber off
                dLogs.any { it.status == "attended" } -> 0xFF34D399.toInt() // Mint attended
                else -> 0x3394A3B8
            }
            dayStatuses.add(Triple(d.dayOfMonth.toString(), statusColor, d.dayOfWeek.name.take(1)))
        }

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(18.dp)
                    .background(Color(0xFF05070B))
                    .padding(14.dp)
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = "CALIPER • ATTENDANCE MATRIX",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF38BDF8)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Last 14 Days Collegiate History",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF94A3B8)),
                                    fontSize = 10.sp
                                )
                            )
                        }

                        val pctColor = if (safeZonePct >= 75.0) Color(0xFF34D399) else Color(0xFFFB7185)
                        Text(
                            text = "${String.format("%.1f", safeZonePct)}%",
                            style = TextStyle(
                                color = ColorProvider(pctColor),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    // 2 rows of 7 days
                    val row1 = dayStatuses.take(7)
                    val row2 = dayStatuses.drop(7)

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (item in row1) {
                            Column(
                                modifier = GlanceModifier.defaultWeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = GlanceModifier
                                        .size(24.dp)
                                        .cornerRadius(6.dp)
                                        .background(Color(item.second)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.first,
                                        style = TextStyle(
                                            color = ColorProvider(Color(0xFF05070B)),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Text(
                                    text = item.third,
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFF64748B)),
                                        fontSize = 8.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (item in row2) {
                            Column(
                                modifier = GlanceModifier.defaultWeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = GlanceModifier
                                        .size(24.dp)
                                        .cornerRadius(6.dp)
                                        .background(Color(item.second)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.first,
                                        style = TextStyle(
                                            color = ColorProvider(Color(0xFF05070B)),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Text(
                                    text = item.third,
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFF64748B)),
                                        fontSize = 8.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    // Legend
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "● Present",
                            style = TextStyle(color = ColorProvider(Color(0xFF34D399)), fontSize = 9.sp)
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = "● Bunk",
                            style = TextStyle(color = ColorProvider(Color(0xFFFB7185)), fontSize = 9.sp)
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = "● Off/Cancelled",
                            style = TextStyle(color = ColorProvider(Color(0xFFFBBF24)), fontSize = 9.sp)
                        )
                    }
                }
            }
        }
    }
}

class CaliperMatrixWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CaliperMatrixWidget()
}
