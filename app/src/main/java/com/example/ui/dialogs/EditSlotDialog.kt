package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TimetableSlotEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@Composable
fun EditSlotDialog(
    slot: TimetableSlotEntity,
    subjects: List<SubjectEntity>,
    onDismissRequest: () -> Unit,
    onSaveSlot: (TimetableSlotEntity) -> Unit,
    onDeleteSlot: (Long) -> Unit
) {
    val context = LocalContext.current
    var selectedSubjectId by remember { mutableStateOf(slot.subjectId) }
    var startTime by remember { mutableStateOf(slot.startTime) }
    var endTime by remember { mutableStateOf(slot.endTime) }
    var roomNo by remember { mutableStateOf(slot.roomNo) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismissRequest) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            backgroundColor = Color(0xF20C0E14),
            borderColors = listOf(IceBlue.copy(alpha = 0.6f), GlassBorderBottom)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Edit Period Slot",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Modify timing, room number, or reassign course",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                // Select Subject
                Text("Select Subject", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = IceBlue)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    subjects.forEach { sub ->
                        val isSelected = (sub.id == selectedSubjectId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) IceBlue.copy(alpha = 0.18f) else Color(0x0DFFFFFF),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) IceBlue else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    CaliperSoundManager.playSnap()
                                    CaliperHapticManager.tick(context)
                                    selectedSubjectId = sub.id
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${sub.code} - ${sub.name}",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) IceBlue else TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Time fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IceBlue,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End (HH:mm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IceBlue,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                // Room field
                OutlinedTextField(
                    value = roomNo,
                    onValueChange = { roomNo = it },
                    label = { Text("Room No / Hall") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IceBlue,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Delete confirmation or button
                if (!showDeleteConfirm) {
                    TextButton(
                        onClick = {
                            CaliperSoundManager.playAlert()
                            showDeleteConfirm = true
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = SoftCoral)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Slot from Timetable", fontSize = 12.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onDeleteSlot(slot.id)
                                onDismissRequest()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftCoral),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Confirm Delete", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showDeleteConfirm = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FFFFFF)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSaveSlot(
                                slot.copy(
                                    subjectId = selectedSubjectId,
                                    startTime = startTime,
                                    endTime = endTime,
                                    roomNo = roomNo
                                )
                            )
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IceBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
