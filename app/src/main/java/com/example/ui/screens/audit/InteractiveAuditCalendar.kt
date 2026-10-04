package com.example.ui.screens.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.ui.MainViewModel
import com.example.ui.theme.CaliperTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

typealias CampusViewModel = MainViewModel

@Composable
fun InteractiveAuditCalendar(
    viewModel: CampusViewModel,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialPage = remember { 1200 }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2400 })
    val coroutineScope = rememberCoroutineScope()
    
    val currentMonth = remember(pagerState.currentPage) {
        YearMonth.now().plusMonths((pagerState.currentPage - initialPage).toLong())
    }
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = Color.White)
            }

            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showPicker = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color.White)
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val month = remember(page) {
                YearMonth.now().plusMonths((page - initialPage).toLong())
            }
            MonthGrid(
                month = month,
                viewModel = viewModel,
                onDateSelected = onDateSelected
            )
        }
    }

    if (showPicker) {
        val currentYear = remember { LocalDate.now().year }
        val semesterMonths = listOf(
            YearMonth.of(currentYear, 7),
            YearMonth.of(currentYear, 8),
            YearMonth.of(currentYear, 9),
            YearMonth.of(currentYear, 10),
            YearMonth.of(currentYear, 11),
            YearMonth.of(currentYear, 12)
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            containerColor = Color(0xFF111622),
            title = { Text("Select Semester Month", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    semesterMonths.forEach { ym ->
                        val isChosen = (ym == currentMonth)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isChosen) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    val targetPage = initialPage + ((ym.year - YearMonth.now().year) * 12 + (ym.monthValue - YearMonth.now().monthValue))
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(targetPage)
                                    }
                                    showPicker = false
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                color = if (isChosen) Color.White else Color(0xFF94A3B8),
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun MonthGrid(
    month: YearMonth,
    viewModel: CampusViewModel,
    onDateSelected: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors
    val daysInMonth = remember(month) { month.lengthOfMonth() }
    val firstDayOffset = remember(month) { month.atDay(1).dayOfWeek.value - 1 }
    val logs by viewModel.logs.collectAsState()
    val today = remember { LocalDate.now() }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        // Weekday header
        val dayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayHeaders.forEach { d ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(d, color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

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
                        val cellDate = month.atDay(dayNum)
                        val dateStr = cellDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val dayLogs = logs.filter { it.date == dateStr }
                        val isToday = cellDate.isEqual(today)

                        val attended = dayLogs.count { it.status == "attended" }
                        val bunked = dayLogs.count { it.status == "bunked" }
                        val dotColor = when {
                            attended > 0 && bunked == 0 -> colors.safeZone
                            attended > 0 && bunked > 0 -> colors.deadlineWarning
                            bunked > 0 -> colors.bunkDanger
                            else -> Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isToday) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f))
                                .border(
                                    0.5.dp,
                                    if (isToday) colors.primaryAccent else Color.White.copy(alpha = 0.05f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    CaliperSoundManager.playSnap()
                                    CaliperHapticManager.tick(context)
                                    onDateSelected(cellDate)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$dayNum",
                                    color = if (isToday) Color.White else Color(0xFFF8FAFC),
                                    fontSize = 12.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                )
                                if (dotColor != Color.Transparent) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).padding(2.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
        }
    }
}
