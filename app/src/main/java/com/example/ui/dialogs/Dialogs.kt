package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.MedicalLeaveEntity
import com.example.data.local.entity.SubjectEntity
import com.example.ui.SubjectAttendanceStats
import com.example.ui.components.GlassCard
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.fluidTouchFeedback
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubjectDialog(
    existingSubject: SubjectEntity? = null,
    onDismiss: () -> Unit,
    onSave: (SubjectEntity) -> Unit
) {
    val colors = CaliperTheme.colors
    var code by remember { mutableStateOf(existingSubject?.code ?: "") }
    var name by remember { mutableStateOf(existingSubject?.name ?: "") }
    var faculty by remember { mutableStateOf(existingSubject?.facultyName ?: "") }
    var type by remember { mutableStateOf(existingSubject?.type ?: "THEORY") }
    var strictness by remember { mutableStateOf(existingSubject?.strictness ?: "MODERATE") }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (existingSubject == null) "ADD SUBJECT" else "EDIT SUBJECT",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Subject Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = faculty,
                    onValueChange = { faculty = it },
                    label = { Text("Faculty Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("THEORY", "LAB").forEach { item ->
                        FilterChip(
                            selected = type.equals(item, ignoreCase = true),
                            onClick = { type = item },
                            label = { Text(item) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("STRICT", "MODERATE", "CHILL").forEach { item ->
                        FilterChip(
                            selected = strictness.equals(item, ignoreCase = true),
                            onClick = { strictness = item },
                            label = { Text(item) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            if (code.isNotBlank() && name.isNotBlank()) {
                                onSave(
                                    SubjectEntity(
                                        id = existingSubject?.id ?: 0L,
                                        code = code.trim(),
                                        name = name.trim(),
                                        facultyName = faculty.trim().ifEmpty { "TBA" },
                                        type = type.lowercase(),
                                        strictness = strictness.lowercase(),
                                        reconciledAttendedOffset = existingSubject?.reconciledAttendedOffset ?: 0,
                                        reconciledTotalOffset = existingSubject?.reconciledTotalOffset ?: 0
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconciliationDialog(
    stats: SubjectAttendanceStats,
    onDismiss: () -> Unit,
    onReconcile: (Int, Int) -> Unit
) {
    val colors = CaliperTheme.colors
    var attendedStr by remember { mutableStateOf(stats.attended.toString()) }
    var conductedStr by remember { mutableStateOf(stats.totalConducted.toString()) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "RECONCILE REGISTER",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stats.subject.code} • ${stats.subject.name}",
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = attendedStr,
                    onValueChange = { attendedStr = it },
                    label = { Text("Total Attended") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = conductedStr,
                    onValueChange = { conductedStr = it },
                    label = { Text("Total Conducted") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            val att = attendedStr.toIntOrNull() ?: stats.attended
                            val cond = (conductedStr.toIntOrNull() ?: stats.totalConducted).coerceAtLeast(att)
                            onReconcile(att, cond)
                        }
                    ) {
                        Text("Sync Tally")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraClassDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onLogExtra: (Long, String, Boolean, String?) -> Unit
) {
    val colors = CaliperTheme.colors
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
    var status by remember { mutableStateOf("PRESENT") }
    var isProxy by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "LOG EXTRA CLASS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                subjects.take(6).forEach { subj ->
                    val selected = subj.id == selectedSubjectId
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) colors.scheduledLecture.copy(alpha = 0.22f)
                                else colors.containerSurface
                            )
                            .fluidTouchFeedback(onClick = { selectedSubjectId = subj.id })
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "${subj.code} • ${subj.name}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                color = colors.textPrimary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PRESENT", "ABSENT").forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Topic / Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            if (selectedSubjectId != 0L) {
                                onLogExtra(selectedSubjectId, status, isProxy, notes.ifBlank { null })
                            }
                        }
                    ) {
                        Text("Log Class")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAssessmentDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (AssessmentEntity) -> Unit
) {
    val colors = CaliperTheme.colors
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("CLASS_TEST") }
    var dueDate by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 1L) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "ADD ASSESSMENT",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Assessment Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    AssessmentEntity(
                                        subjectId = selectedSubjectId,
                                        type = category,
                                        title = title.trim(),
                                        dueDate = dueDate.trim(),
                                        status = "UPCOMING"
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicalLeaveDialog(
    onDismiss: () -> Unit,
    onSave: (MedicalLeaveEntity) -> Unit
) {
    val colors = CaliperTheme.colors
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(2).toString()) }
    var reason by remember { mutableStateOf("") }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "LOG MEDICAL AFFIDAVIT",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason / Diagnosis") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            if (reason.isNotBlank()) {
                                onSave(
                                    MedicalLeaveEntity(
                                        startDate = startDate.trim(),
                                        endDate = endDate.trim(),
                                        reason = reason.trim()
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Save Affidavit")
                    }
                }
            }
        }
    }
}
