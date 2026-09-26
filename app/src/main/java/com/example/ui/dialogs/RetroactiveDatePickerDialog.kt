package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun RetroactiveDatePickerDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val parsedInitial = remember(initialDate) {
        try {
            LocalDate.parse(initialDate, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            LocalDate.now()
        }
    }

    var viewingMonth by remember { mutableStateOf(YearMonth.from(parsedInitial)) }
    val today = remember { LocalDate.now() }

    val daysInMonth = viewingMonth.lengthOfMonth()
    val firstDayOffset = viewingMonth.atDay(1).dayOfWeek.value - 1 // 0 = Mon, 6 = Sun

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = AmoledCardSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorderTop, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(WarningAmber.copy(alpha = 0.2f))
                                .border(1.dp, WarningAmber, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EventRepeat, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Retroactive Calendar",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tap any past date to log attendance",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Month switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x221F293D))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewingMonth = viewingMonth.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month", tint = NeonCyan)
                    }

                    Text(
                        text = viewingMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    IconButton(onClick = { viewingMonth = viewingMonth.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month", tint = NeonCyan)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Day headers Mon..Sun
                val dayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    dayHeaders.forEach { h ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(h, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Month calendar grid
                val totalCells = firstDayOffset + daysInMonth
                val rowCount = (totalCells + 6) / 7

                for (r in 0 until rowCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (c in 0..6) {
                            val cellIndex = r * 7 + c
                            val dayNum = cellIndex - firstDayOffset + 1

                            if (dayNum in 1..daysInMonth) {
                                val cellDate = viewingMonth.atDay(dayNum)
                                val dateStr = cellDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                val isCellToday = cellDate.isEqual(today)
                                val isCellPast = cellDate.isBefore(today)
                                val isCellSelected = (dateStr == initialDate)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isCellSelected -> NeonCyan.copy(alpha = 0.35f)
                                                isCellToday -> NeonCyan.copy(alpha = 0.15f)
                                                isCellPast -> Color(0x18FFFFFF)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            when {
                                                isCellSelected -> NeonCyan
                                                isCellToday -> NeonCyan.copy(alpha = 0.8f)
                                                else -> Color.Transparent
                                            },
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            triggerHapticFeedback(context, false)
                                            onDateSelected(dateStr)
                                            onDismiss()
                                        }
                                        .testTag("calendar_day_$dayNum"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$dayNum",
                                            color = when {
                                                isCellSelected -> NeonCyan
                                                isCellToday -> NeonCyan
                                                isCellPast -> TextPrimary
                                                else -> TextMuted
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = if (isCellSelected || isCellToday) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (isCellToday) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(NeonCyan)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        onDateSelected(today.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        onDismiss()
                    }) {
                        Text("Jump to Today", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "Past dates are fully editable",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
