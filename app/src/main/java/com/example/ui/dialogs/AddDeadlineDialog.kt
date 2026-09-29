package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.SubjectEntity
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AddDeadlineDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (AssessmentEntity) -> Unit
) {
    val types = listOf(
        "drawing_sheet" to "M/C Drawing Sheet",
        "workshop_job" to "Workshop Job",
        "lab_file" to "Lab File / Report",
        "assignment" to "Class Assignment",
        "sessional_1" to "Sessional Exam",
        "ct" to "Surprise Class Test"
    )

    var selectedType by remember { mutableStateOf(types.first().first) }
    var selectedSubjectId by remember(subjects) { mutableStateOf(subjects.firstOrNull()?.id ?: 1L) }
    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(LocalDate.now().plusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var dueTime by remember { mutableStateOf("17:00") }
    var maxMarks by remember { mutableStateOf("10") }

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
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Submission Deadline",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Text("Module / Submission Category", fontSize = 12.sp, color = TextSecondary)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.chunked(2).forEach { rowTypes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowTypes.forEach { (typeId, label) ->
                                val isSelected = (selectedType == typeId)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0x1AFFFFFF))
                                        .border(1.dp, if (isSelected) NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { selectedType = typeId }
                                        .padding(vertical = 8.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) NeonCyan else TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Text("Link to Subject", fontSize = 12.sp, color = TextSecondary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    subjects.forEach { s ->
                        val isSelected = (s.id == selectedSubjectId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color(0x12FFFFFF))
                                .clickable { selectedSubjectId = s.id }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedSubjectId = s.id },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${s.code} • ${s.name}", color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                }

                Text("Submission Title / Job Spec", fontSize = 12.sp, color = TextSecondary)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Sheet 2: Riveted Joints / Lathe Job 1", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderTop
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Target Date", fontSize = 11.sp, color = TextSecondary)
                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GlassBorderTop
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Target Time (HH:mm)", fontSize = 11.sp, color = TextSecondary)
                        OutlinedTextField(
                            value = dueTime,
                            onValueChange = { dueTime = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = GlassBorderTop
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Text("Max Marks / Weightage", fontSize = 11.sp, color = TextSecondary)
                OutlinedTextField(
                    value = maxMarks,
                    onValueChange = { maxMarks = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderTop
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(
                                AssessmentEntity(
                                    subjectId = selectedSubjectId,
                                    type = selectedType,
                                    title = title.trim(),
                                    maxMarks = maxMarks.toDoubleOrNull() ?: 10.0,
                                    status = "pending",
                                    dueDate = dueDate.trim(),
                                    dueTime = dueTime.trim().ifEmpty { "17:00" },
                                    syllabusCoveragePercent = 25
                                )
                            )
                            onDismiss()
                        }
                    },
                    enabled = title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Deadline", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
