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
import com.example.data.local.entity.AcademicTaskEntity
import com.example.data.local.entity.SubjectEntity
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AddTaskDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (AcademicTaskEntity) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("Medium") } // High, Medium, Low
    var selectedCategory by remember { mutableStateOf("Sheet Work") } // Sheet Work, Workshop, Exam Prep, Assignment
    var dueDate by remember { mutableStateOf(LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var selectedSubjectId by remember(subjects) { mutableStateOf(subjects.firstOrNull()?.id) }

    val priorities = listOf("High" to "Urgent", "Medium" to "Normal", "Low" to "Low")
    val categories = listOf("Sheet Work", "Workshop Job", "Exam Prep", "Assignment", "Lab Report")

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
                        text = "New Academic Task",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Text("Task Description", fontSize = 12.sp, color = TextSecondary)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("e.g. Complete Sheet 3 Isometric View / Revise Unit 2", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderTop
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Priority Level", fontSize = 12.sp, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    priorities.forEach { (pKey, label) ->
                        val isSelected = (selectedPriority == pKey)
                        val pColor = when (pKey) {
                            "High" -> StrictRed
                            "Medium" -> NeonCyan
                            else -> TextMuted
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) pColor.copy(alpha = 0.2f) else Color(0x1AFFFFFF))
                                .border(1.dp, if (isSelected) pColor else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { selectedPriority = pKey }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (isSelected) pColor else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("Category", fontSize = 12.sp, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        val isSelected = (selectedCategory == cat)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.18f) else Color(0x12FFFFFF))
                                .border(1.dp, if (isSelected) NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = cat }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cat, color = if (isSelected) NeonCyan else TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }
                }

                Text("Target Due Date", fontSize = 12.sp, color = TextSecondary)
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

                if (subjects.isNotEmpty()) {
                    Text("Associated Subject (Optional)", fontSize = 12.sp, color = TextSecondary)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        subjects.take(4).forEach { s ->
                            val isSelected = (selectedSubjectId == s.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { selectedSubjectId = if (isSelected) null else s.id }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedSubjectId = if (isSelected) null else s.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${s.code} • ${s.name}", color = TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (description.isNotBlank()) {
                            onSave(
                                AcademicTaskEntity(
                                    description = description.trim(),
                                    priority = selectedPriority,
                                    category = selectedCategory,
                                    dueDate = dueDate.trim(),
                                    isCompleted = false,
                                    subjectId = selectedSubjectId
                                )
                            )
                            onDismiss()
                        }
                    },
                    enabled = description.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Task", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
