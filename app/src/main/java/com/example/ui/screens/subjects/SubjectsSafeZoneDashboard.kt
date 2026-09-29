package com.example.ui.screens.subjects

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.local.entity.SubjectEntity
import com.example.ui.MainViewModel
import com.example.ui.SubjectAttendanceStats
import com.example.ui.components.*
import com.example.ui.dialogs.AddEditSubjectDialog
import com.example.ui.dialogs.ReconciliationDialog
import com.example.ui.theme.*

@Composable
fun SubjectsSafeZoneDashboard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val statsList by viewModel.subjectStats.collectAsState()
    val summary by viewModel.globalSummary.collectAsState()

    var filterMode by remember { mutableStateOf("all") } // "all", "risk", "theory", "practical"
    var reconcilingSubject by remember { mutableStateOf<SubjectAttendanceStats?>(null) }
    var editingSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredList = remember(statsList, filterMode) {
        when (filterMode) {
            "risk" -> statsList.filter { it.percentage < 75.0 }
            "theory" -> statsList.filter { it.subject.type.lowercase() == "theory" }
            "practical" -> statsList.filter { it.subject.type.lowercase() != "theory" }
            else -> statsList
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Global Metric Glass Ring Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_metric_ring_card"),
                borderColors = listOf(Color(0x6600E5FF), GlassBorderBottom)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SAFE-ZONE AGGREGATE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Semester Eligibility",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "IERT Prayagraj 75% Rule",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mini metrics tally
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column {
                                Text("${summary.totalAttended} / ${summary.totalConducted}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Attended", fontSize = 10.sp, color = TextMuted)
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0x33FFFFFF)))
                            Column {
                                Text("${summary.totalProxy}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = IceSky)
                                Text("Proxies", fontSize = 10.sp, color = TextMuted)
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0x33FFFFFF)))
                            Column {
                                Text("${summary.totalFacultyCancelled}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WarningAmber)
                                Text("Cancelled", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }

                    MetricRadialRing(
                        percentage = summary.overallPercentage,
                        size = 120.dp,
                        strokeWidth = 10.dp
                    )
                }
            }
        }

        // 2. Detained / Admit Card Risk Alert Banner
        if (summary.atRiskSubjectCount > 0 || summary.criticalSubjectCount > 0) {
            item {
                val atRiskNames = statsList.filter { it.percentage < 75.0 }.joinToString { it.subject.code }
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detained_risk_alert_banner"),
                    backgroundColor = if (summary.criticalSubjectCount > 0) DangerRed else WarningAmber,
                    borderColors = listOf(StrictRed, GlassBorderBottom)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StrictRed.copy(alpha = 0.25f))
                                .border(1.dp, StrictRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = StrictRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = if (summary.criticalSubjectCount > 0) "ADMIT CARD DETENTION RISK!" else "ATTENDANCE SAFE-ZONE WARNING",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (summary.criticalSubjectCount > 0) StrictRed else WarningAmber,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${summary.atRiskSubjectCount + summary.criticalSubjectCount} subjects ($atRiskNames) are under the mandatory 75% cutoff. Attend subsequent classes immediately to avoid exam hall bar.",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Category Filter Chips & Add Subject Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val filters = listOf(
                        "all" to "All (${statsList.size})",
                        "risk" to "At Risk (${statsList.count { it.percentage < 75.0 }})",
                        "theory" to "Theory",
                        "practical" to "Lab & Workshop"
                    )

                    filters.forEach { (mode, label) ->
                        val isSelected = (filterMode == mode)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0x1AFFFFFF))
                                .border(
                                    1.dp,
                                    if (isSelected) NeonCyan else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    triggerHapticFeedback(context, false)
                                    filterMode = mode
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else TextSecondary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Add Subject",
                        tint = NeonCyan
                    )
                }
            }
        }

        // 4. Subject-Specific Glass Cards
        items(filteredList, key = { it.subject.id }) { stat ->
            SubjectGlassCard(
                stat = stat,
                onReconcileClick = { reconcilingSubject = stat },
                onEditClick = { editingSubject = stat.subject },
                onQuickAdjust = { deltaAtt, deltaCond ->
                    viewModel.reconcileSubject(
                        subjectId = stat.subject.id,
                        officialAttended = stat.attended + deltaAtt,
                        officialTotal = stat.totalConducted + deltaCond
                    )
                }
            )
        }
    }

    // Reconciliation Dialog
    reconcilingSubject?.let { stat ->
        ReconciliationDialog(
            stats = stat,
            onDismiss = { reconcilingSubject = null },
            onConfirm = { offAtt, offTot ->
                viewModel.reconcileSubject(stat.subject.id, offAtt, offTot)
            }
        )
    }

    // Add / Edit Subject Dialog
    if (showAddDialog || editingSubject != null) {
        AddEditSubjectDialog(
            initialSubject = editingSubject,
            onDismiss = {
                showAddDialog = false
                editingSubject = null
            },
            onSave = { subject ->
                viewModel.saveSubject(subject)
            }
        )
    }
}

@Composable
fun SubjectGlassCard(
    stat: SubjectAttendanceStats,
    onReconcileClick: () -> Unit,
    onEditClick: () -> Unit,
    onQuickAdjust: (deltaAttended: Int, deltaTotal: Int) -> Unit
) {
    val context = LocalContext.current
    val sub = stat.subject
    val percentage = stat.percentage
    val isSafe = percentage >= 75.0

    val statusColor = when {
        percentage >= 75.0 -> NeonEmerald
        percentage >= 65.0 -> WarningAmber
        else -> StrictRed
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColors = listOf(
            if (isSafe) GlassBorderTop else statusColor.copy(alpha = 0.7f),
            GlassBorderBottom
        )
    ) {
        // Row 1: Code, Type, Strictness & Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x2200E5FF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(sub.code, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(6.dp))
                TypeBadge(sub.type)
                Spacer(modifier = Modifier.width(6.dp))
                StrictnessBadge(sub.strictness)
            }

            Row {
                IconButton(onClick = onReconcileClick, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Reconcile with Register",
                        tint = IceSky,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onEditClick, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Subject",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Subject Title & Faculty
        Text(
            text = sub.name,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Faculty: ${sub.facultyName}",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Row 3: Progress Bar & Percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "${stat.attended} Attended / ${stat.totalConducted} Conducted",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format("%.1f%%", percentage),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Custom Frosted Progress Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x33FFFFFF))
        ) {
            val progressFraction = (percentage.toFloat() / 100f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = progressFraction)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(statusColor.copy(alpha = 0.7f), statusColor)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Granular Breakdown Badges: Attended, Bunked, Faculty Cancelled, College Off
        val bunkedCount = kotlin.math.max(0, stat.totalConducted - stat.attended)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1A00E676))
                    .border(0.5.dp, NeonEmerald.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${stat.attended}", color = NeonEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Attended", color = TextSecondary, fontSize = 9.sp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1AEF4444))
                    .border(0.5.dp, StrictRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$bunkedCount", color = StrictRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Bunked", color = TextSecondary, fontSize = 9.sp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1AF59E0B))
                    .border(0.5.dp, WarningAmber.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${stat.facultyCancelled}", color = WarningAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Cancelled", color = TextSecondary, fontSize = 9.sp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(0.5.dp, GlassBorderTop, RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${stat.collegeOff}", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("College Off", color = TextSecondary, fontSize = 9.sp)
                }
            }
        }

        val totalExcluded = stat.facultyCancelled + stat.collegeOff
        if (totalExcluded > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "ℹ️ $totalExcluded cancelled/off lectures excluded from total conducted (0% penalty on 75% rule).",
                color = TextMuted,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 4: Predictive Bunk / Catch-up Engine Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSafe) Color(0x1400E676) else Color(0x1DEF4444))
                .border(
                    0.5.dp,
                    if (isSafe) NeonEmerald.copy(alpha = 0.4f) else StrictRed.copy(alpha = 0.4f),
                    RoundedCornerShape(12.dp)
                )
                .padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isSafe) Icons.Default.BeachAccess else Icons.Default.RunningWithErrors,
                    contentDescription = null,
                    tint = if (isSafe) NeonEmerald else StrictRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    if (isSafe) {
                        Text(
                            text = if (stat.bunksAvailable > 0) {
                                "Safe Zone: You can skip ${stat.bunksAvailable} consecutive classes and stay ≥ 75%"
                            } else {
                                "On The Edge: Don't miss next lecture (0 bunks available)"
                            },
                            color = NeonEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Detention Risk: Attend next ${stat.classesNeeded} consecutive classes to reach 75%!",
                            color = StrictRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Real-time sync based on current 75% statutory rule.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 5: Quick Adjust buttons for instant register correction
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onReconcileClick) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = IceSky, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Register Reconcile", color = IceSky, fontSize = 11.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        triggerHapticFeedback(context, false)
                        onQuickAdjust(1, 1)
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonEmerald)
                ) {
                    Text("+1 Present", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        triggerHapticFeedback(context, false)
                        onQuickAdjust(0, 1)
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StrictRed)
                ) {
                    Text("+1 Bunk", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
