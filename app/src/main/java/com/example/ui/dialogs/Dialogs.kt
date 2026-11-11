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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.AssessmentEntity
import com.example.data.local.entity.MedicalLeaveEntity
import com.example.data.local.entity.SubjectEntity
import com.example.ui.SubjectAttendanceStats
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ExtraClassDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onConfirm: (subjectId: Long, status: String, isProxy: Boolean, notes: String) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 1L) }
    var selectedStatus by remember { mutableStateOf("attended") }
    var isProxy by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

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
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Log Extra / Ad-Hoc Class",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Select Subject", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                subjects.forEach { sub ->
                    val isSelected = (sub.id == selectedSubjectId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color(0x1AFFFFFF))
                            .border(
                                1.dp,
                                if (isSelected) NeonCyan else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedSubjectId = sub.id }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedSubjectId = sub.id },
                            colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(sub.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("${sub.code} • ${sub.facultyName}", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Attendance Status", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val statusOptions = listOf("attended" to "Attended", "bunked" to "Bunked")
                    statusOptions.forEach { (st, label) ->
                        val isChosen = (selectedStatus == st)
                        Button(
                            onClick = { selectedStatus = st },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isChosen) {
                                    if (st == "attended") NeonEmerald else DangerRed
                                } else Color(0x22FFFFFF)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label, color = if (isChosen) Color.Black else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (selectedStatus == "attended") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x1A00E5FF))
                            .padding(8.dp)
                    ) {
                        Checkbox(
                            checked = isProxy,
                            onCheckedChange = { isProxy = it },
                            colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Marked by Proxy", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Maintains roll sync while tracking physical presence", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (e.g. Compensatory Lecture)", color = TextSecondary, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderTop
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onConfirm(selectedSubjectId, selectedStatus, isProxy, notes)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("confirm_extra_class")
                ) {
                    Text("Add Ad-Hoc Class", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReconciliationDialog(
    stats: SubjectAttendanceStats,
    onDismiss: () -> Unit,
    onConfirm: (officialAttended: Int, officialTotal: Int) -> Unit
) {
    var officialAttendedText by remember { mutableStateOf(stats.attended.toString()) }
    var officialTotalText by remember { mutableStateOf(stats.totalConducted.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = AmoledCardSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorderTop, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = "Sync with Professor's Register",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stats.subject.name} (${stats.subject.code})",
                    fontSize = 12.sp,
                    color = NeonCyan
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "If teacher announced official roll totals, enter them below. The app will calculate the exact reconciliation offset instantly without losing daily logs.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = officialAttendedText,
                    onValueChange = { officialAttendedText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Teacher's Roll: Present Count", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderTop
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = officialTotalText,
                    onValueChange = { officialTotalText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Teacher's Roll: Total Classes Conducted", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderTop
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val att = officialAttendedText.toIntOrNull() ?: stats.attended
                            val tot = officialTotalText.toIntOrNull() ?: stats.totalConducted
                            onConfirm(att, tot)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).testTag("reconcile_confirm_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reconcile", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditSubjectDialog(
    initialSubject: SubjectEntity? = null,
    onDismiss: () -> Unit,
    onSave: (SubjectEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialSubject?.name ?: "") }
    var code by remember { mutableStateOf(initialSubject?.code ?: "") }
    var facultyName by remember { mutableStateOf(initialSubject?.facultyName ?: "") }
    var type by remember { mutableStateOf(initialSubject?.type ?: "theory") }
    var strictness by remember { mutableStateOf(initialSubject?.strictness ?: "moderate") }

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
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialSubject == null) "Add Engineering Subject" else "Edit Subject",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name (e.g. Thermodynamics)", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderTop
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Code (e.g. ME-301)", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderTop
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = facultyName,
                    onValueChange = { facultyName = it },
                    label = { Text("Faculty In-Charge", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderTop
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("Subject Type", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("theory" to "Theory", "lab" to "Lab", "workshop" to "Workshop").forEach { (tp, lbl) ->
                        val isChosen = (type == tp)
                        Button(
                            onClick = { type = tp },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isChosen) NeonCyan else Color(0x22FFFFFF)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(lbl, color = if (isChosen) Color.Black else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Faculty Strictness", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("strict" to "Strict", "moderate" to "Moderate", "chill" to "Chill").forEach { (st, lbl) ->
                        val isChosen = (strictness == st)
                        val color = if (st == "strict") StrictRed else if (st == "moderate") ModerateAmber else ChillEmerald
                        Button(
                            onClick = { strictness = st },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isChosen) color else Color(0x22FFFFFF)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(lbl, color = if (isChosen) Color.Black else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val entity = initialSubject?.copy(
                                name = name,
                                code = code,
                                facultyName = facultyName,
                                type = type,
                                strictness = strictness
                            ) ?: SubjectEntity(
                                name = name,
                                code = code,
                                facultyName = facultyName,
                                type = type,
                                strictness = strictness,
                                colorHex = "#00E5FF"
                            )
                            onSave(entity)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_subject_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Subject", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddAssessmentDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (AssessmentEntity) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 1L) }
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("sessional_1") }
    var maxMarks by remember { mutableStateOf("30") }
    var obtainedMarks by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("in_progress") }

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
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Add Academic Assessment", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Sessional 2 or Sheet 5)", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderTop
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text("Category", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                val types = listOf(
                    "sessional_1" to "Sessional 1",
                    "sessional_2" to "Sessional 2",
                    "sessional_3" to "Sessional 3",
                    "ct" to "Class Test",
                    "drawing_sheet" to "Drawing Sheet",
                    "workshop_job" to "Workshop Job"
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    types.chunked(2).forEach { pair ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            pair.forEach { (tp, lbl) ->
                                val isChosen = (type == tp)
                                Button(
                                    onClick = { type = tp },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isChosen) NeonCyan else Color(0x22FFFFFF)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(lbl, color = if (isChosen) Color.Black else TextPrimary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = maxMarks,
                        onValueChange = { maxMarks = it },
                        label = { Text("Max Marks", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = GlassBorderTop
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = obtainedMarks,
                        onValueChange = { obtainedMarks = it },
                        label = { Text("Scored (Optional)", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = GlassBorderTop
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val maxM = maxMarks.toDoubleOrNull() ?: 30.0
                        val obtM = obtainedMarks.toDoubleOrNull()
                        val stat = if (obtM != null) "appeared" else status
                        val entity = AssessmentEntity(
                            subjectId = selectedSubjectId,
                            title = if (title.isNotBlank()) title else "Assessment",
                            type = type,
                            maxMarks = maxM,
                            obtainedMarks = obtM,
                            status = stat,
                            dueDate = LocalDate.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE)
                        )
                        onSave(entity)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_assessment_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Assessment", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddMedicalLeaveDialog(
    onDismiss: () -> Unit,
    onSave: (MedicalLeaveEntity) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var doctorName by remember { mutableStateOf("") }
    var refNo by remember { mutableStateOf("") }
    var daysCount by remember { mutableStateOf("3") }

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
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Log Medical / HOD Leave Slip", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Diagnosis / Reason (e.g. Viral Fever)", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderTop
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text("Doctor / Hospital (e.g. Beli Hospital)", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderTop
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = refNo,
                    onValueChange = { refNo = it },
                    label = { Text("Application / Slip No. (e.g. IERT/MED/092)", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderTop
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val today = LocalDate.now()
                        val dtf = DateTimeFormatter.ISO_LOCAL_DATE
                        val days = daysCount.toIntOrNull() ?: 3
                        val entity = MedicalLeaveEntity(
                            startDate = today.minusDays(days.toLong()).format(dtf),
                            endDate = today.format(dtf),
                            reason = if (reason.isNotBlank()) reason else "Medical Leave",
                            doctorName = if (doctorName.isNotBlank()) doctorName else "Authorized Medical Practitioner",
                            submittedTo = "HOD Mechanical Engineering, IERT",
                            status = "submitted",
                            refNo = if (refNo.isNotBlank()) refNo else "IERT/MED/${System.currentTimeMillis() % 1000}"
                        )
                        onSave(entity)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_medical_leave_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Submit Application to Vault", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
