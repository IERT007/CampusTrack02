package com.example.ui.screens.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlusJakartaFamily
import com.example.ui.theme.fluidTouchFeedback
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InteractiveAuditCalendar(
    viewModel: MainViewModel,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CaliperTheme.colors
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.US)} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Row {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = colors.textSecondary
                        )
                    }
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = colors.textSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            MonthGrid(
                yearMonth = currentMonth,
                viewModel = viewModel,
                onDateSelected = onDateSelected
            )
        }
    }
}

@Composable
fun MonthGrid(
    yearMonth: YearMonth,
    viewModel: MainViewModel,
    onDateSelected: (String) -> Unit
) {
    val colors = CaliperTheme.colors
    val logs by viewModel.logs.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val logsByDate = remember(logs) { logs.groupBy { it.date } }

    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val headers = listOf("S", "M", "T", "W", "T", "F", "S")
        Row(modifier = Modifier.fillMaxWidth()) {
            headers.forEach { h ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = h,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PlusJakartaFamily,
                            color = colors.textMuted
                        )
                    )
                }
            }
        }

        for (week in 0 until (totalCells / 7)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (dow in 0 until 7) {
                    val cellIndex = week * 7 + dow
                    val dayNum = cellIndex - firstDayOfWeek + 1
                    if (dayNum in 1..daysInMonth) {
                        val date = yearMonth.atDay(dayNum)
                        val dateStr = date.toString()
                        val dayLogs = logsByDate[dateStr].orEmpty()
                        val isSelected = dateStr == selectedDate

                        val dotColor = when {
                            dayLogs.any { it.status == "ABSENT" } -> colors.bunkDanger
                            dayLogs.any { it.status == "PRESENT" } -> colors.safeZone
                            dayLogs.any { it.status == "COLLEGE_OFF" } -> colors.deadlineWarning
                            else -> Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) colors.scheduledLecture
                                    else Color.Transparent
                                )
                                .fluidTouchFeedback(onClick = { onDateSelected(dateStr) }),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White
                                        else if (date == LocalDate.now()) colors.safeZone
                                        else colors.textPrimary
                                    )
                                )
                                if (dotColor != Color.Transparent) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else dotColor)
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}
