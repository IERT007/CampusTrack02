package com.example.ui.screens.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHapticManager
import com.example.data.local.entity.SubjectEntity
import com.example.ui.MainViewModel
import com.example.ui.SubjectAttendanceStats
import com.example.ui.components.GlassCard
import com.example.ui.components.MetricRadialRing
import com.example.ui.components.SlimVernierPrecisionBar
import com.example.ui.components.StrictnessBadge
import com.example.ui.components.TypeBadge
import com.example.ui.dialogs.AddEditSubjectDialog
import com.example.ui.dialogs.ReconciliationDialog
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlusJakartaFamily
import com.example.ui.theme.fluidTouchFeedback

@Composable
fun SafeZoneScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors
    val subjectStats by viewModel.subjectStats.collectAsState()

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var reconcilingStats by remember { mutableStateOf<SubjectAttendanceStats?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .testTag("safe_zone_screen_list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "safe_zone_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Subject Progress",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    Text(
                        text = "75% Target Buffer & Skip Calculator",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = PlusJakartaFamily,
                            color = colors.textSecondary
                        )
                    )
                }
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .fluidTouchFeedback(
                            onClick = {
                                CaliperHapticManager.tick(context)
                                showAddSubjectDialog = true
                            }
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.scheduledLecture, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp)
                        .testTag("add_subject_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Subject",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Subject",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                }
            }
        }

        items(subjectStats, key = { it.subject.id }) { stats ->
            SubjectCardWithMenu(
                stats = stats,
                onReconcile = { reconcilingStats = stats },
                onEdit = { editingSubject = stats.subject }
            )
        }
    }

    if (showAddSubjectDialog) {
        AddEditSubjectDialog(
            existingSubject = null,
            onDismiss = { showAddSubjectDialog = false },
            onSave = { subj ->
                viewModel.saveSubject(subj)
                showAddSubjectDialog = false
            }
        )
    }

    editingSubject?.let { subj ->
        AddEditSubjectDialog(
            existingSubject = subj,
            onDismiss = { editingSubject = null },
            onSave = { updated ->
                viewModel.saveSubject(updated)
                editingSubject = null
            }
        )
    }

    reconcilingStats?.let { stats ->
        ReconciliationDialog(
            stats = stats,
            onDismiss = { reconcilingStats = null },
            onReconcile = { att, cond ->
                viewModel.reconcileSubject(stats.subject.id, att, cond)
                reconcilingStats = null
            }
        )
    }
}

@Composable
fun SubjectCardWithMenu(
    stats: SubjectAttendanceStats,
    onReconcile: () -> Unit,
    onEdit: () -> Unit
) {
    val colors = CaliperTheme.colors
    val isSafe = stats.percentage >= stats.subject.targetPercentage

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_card_${stats.subject.id}"),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stats.subject.code,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                color = colors.scheduledLecture
                            )
                        )
                        TypeBadge(type = stats.subject.type)
                        StrictnessBadge(strictness = stats.subject.strictness)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stats.subject.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    Text(
                        text = "${stats.subject.facultyName} • ${stats.subject.roomNumber}",
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                MetricRadialRing(percentage = stats.percentage)
            }

            Spacer(modifier = Modifier.height(14.dp))
            SlimVernierPrecisionBar(percentage = stats.percentage.toFloat())
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${stats.attended} of ${stats.totalConducted} Lectures Attended",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                    )
                    Text(
                        text = if (isSafe) {
                            "Safe Buffer: Can skip ${stats.bunksAvailable} lectures"
                        } else {
                            "Recovery: Attend next ${stats.classesNeeded} lectures"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = PlusJakartaFamily,
                            fontSize = 11.sp,
                            color = if (isSafe) colors.safeZone else colors.bunkDanger
                        )
                    )
                }

                Row {
                    IconButton(onClick = onReconcile) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Reconcile Register",
                            tint = colors.textSecondary
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Subject",
                            tint = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
