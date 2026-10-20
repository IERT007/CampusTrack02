package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.PlusJakartaFamily
import com.example.ui.theme.SpaceGroteskFamily
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InteractiveAuditCalendar(
    viewModel: MainViewModel,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CaliperTheme.colors
    val selectedDateStr by viewModel.selectedDate.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var currentYearMonth by remember {
        val initial = runCatching { LocalDate.parse(selectedDateStr) }.getOrElse { LocalDate.now() }
        mutableStateOf(YearMonth.of(initial.year, initial.month))
    }

    // Group logs by date
    val logsByDate = remember(logs) {
        logs.groupBy { it.date }
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Calendar Header with Month Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.US)} ${currentYearMonth.year}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )

                Row {
                    IconButton(
                        onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Month",
                            tint = colors.textSecondary
                        )
                    }
                    IconButton(
                        onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day of week headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayLabel ->
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted
                        ),
                        modifier = Modifier.size(36.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Days Grid
            val firstDay = currentYearMonth.atDay(1)
            val dayOfWeekOffset = (firstDay.dayOfWeek.value - 1) // Monday = 0
            val daysInMonth = currentYearMonth.lengthOfMonth()

            val totalCells = ((dayOfWeekOffset + daysInMonth + 6) / 7) * 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (week in 0 until (totalCells / 7)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (dayIdx in 0..6) {
                            val cellNumber = week * 7 + dayIdx
                            val dayOfMonth = cellNumber - dayOfWeekOffset + 1

                            if (dayOfMonth in 1..daysInMonth) {
                                val date = currentYearMonth.atDay(dayOfMonth)
                                val dateIso = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                val isSelected = dateIso == selectedDateStr
                                val dayLogs = logsByDate[dateIso] ?: emptyList()

                                val hasAttended = dayLogs.any { it.status.equals("ATTENDED", true) || it.status.equals("PRESENT", true) }
                                val hasBunked = dayLogs.any { it.status.equals("BUNKED", true) || it.status.equals("ABSENT", true) }

                                val indicatorColor = when {
                                    hasBunked -> colors.deadlineWarning
                                    hasAttended -> colors.safeZone
                                    else -> Color.Transparent
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) colors.primaryAccent.copy(alpha = 0.25f)
                                            else Color.Transparent
                                        )
                                        .clickable { onDateSelected(dateIso) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayOfMonth.toString(),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = SpaceGroteskFamily,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) colors.primaryAccent else colors.textPrimary,
                                                fontSize = 12.sp
                                            )
                                        )
                                        if (indicatorColor != Color.Transparent) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(indicatorColor)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
