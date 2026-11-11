package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.HolidayRangeEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HolidayManagerDialog(
    holidayRanges: List<HolidayRangeEntity>,
    onDismiss: () -> Unit,
    onSaveHoliday: (HolidayRangeEntity) -> Unit,
    onDeleteHoliday: (Long) -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }

    val todayStr = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
    var title by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(todayStr) }
    var endDate by remember { mutableStateOf(todayStr) }
    var selectedType by remember { mutableStateOf("holiday") } // holiday, strike, semester_break, mass_bunk
    var notes by remember { mutableStateOf("") }

    val types = listOf(
        "holiday" to "Official Holiday",
        "semester_break" to "Mid-Sem Break",
        "strike" to "Institutional Strike",
        "mass_bunk" to "Department Mass Bunk"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AmoledBackground.copy(alpha = 0.95f))
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AmoledCardSurface),
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, GlassBorderTop, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
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
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(WarningAmber.copy(alpha = 0.2f))
                                    .border(1.dp, WarningAmber, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Institutional Holiday Manager", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Automates 'College Off' periods across calendar ranges", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!showAddForm) {
                        Button(
                            onClick = { showAddForm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Add Holiday / Break Range", color = WarningAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (holidayRanges.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.BeachAccess, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No Holiday Ranges Configured", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Add Mid-Sem breaks, Diwali vacation, or strikes to automatically suppress scheduled slots.", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(holidayRanges, key = { it.id }) { h ->
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(WarningAmber.copy(alpha = 0.2f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(h.type.uppercase().replace("_", " "), color = WarningAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(h.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Range: ${h.startDate} to ${h.endDate}", color = IceSky, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                if (!h.notes.isNullOrBlank()) {
                                                    Text(h.notes, color = TextMuted, fontSize = 11.sp)
                                                }
                                            }

                                            IconButton(onClick = { onDeleteHoliday(h.id) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StrictRed)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Add Form
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Event / Holiday Title", color = TextSecondary, fontSize = 12.sp)
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                placeholder = { Text("e.g. Mid-Sem Break / Diwali Vacation", color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = WarningAmber,
                                    unfocusedBorderColor = GlassBorderTop
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Category / Type", color = TextSecondary, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                types.forEach { (typeId, label) ->
                                    val isSelected = (selectedType == typeId)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) WarningAmber.copy(alpha = 0.25f) else Color(0x1AFFFFFF))
                                            .border(1.dp, if (isSelected) WarningAmber else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { selectedType = typeId }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, color = if (isSelected) WarningAmber else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Start Date (YYYY-MM-DD)", color = TextSecondary, fontSize = 11.sp)
                                    OutlinedTextField(
                                        value = startDate,
                                        onValueChange = { startDate = it },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedBorderColor = WarningAmber,
                                            unfocusedBorderColor = GlassBorderTop
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("End Date (YYYY-MM-DD)", color = TextSecondary, fontSize = 11.sp)
                                    OutlinedTextField(
                                        value = endDate,
                                        onValueChange = { endDate = it },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            focusedBorderColor = WarningAmber,
                                            unfocusedBorderColor = GlassBorderTop
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Text("Official Order / Remarks (Optional)", color = TextSecondary, fontSize = 11.sp)
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                placeholder = { Text("e.g. Director Order No. 412 / Registrar Notification", color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = WarningAmber,
                                    unfocusedBorderColor = GlassBorderTop
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showAddForm = false },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Cancel", color = TextSecondary)
                                }

                                Button(
                                    onClick = {
                                        if (title.isNotBlank()) {
                                            onSaveHoliday(
                                                HolidayRangeEntity(
                                                    title = title.trim(),
                                                    startDate = startDate.trim(),
                                                    endDate = endDate.trim(),
                                                    type = selectedType,
                                                    notes = notes.trim().ifEmpty { null }
                                                )
                                            )
                                            title = ""
                                            notes = ""
                                            showAddForm = false
                                        }
                                    },
                                    enabled = title.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Save Holiday", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
