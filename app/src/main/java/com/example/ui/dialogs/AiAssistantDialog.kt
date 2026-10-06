package com.example.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.CaliperAiEngine
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.ui.MainViewModel
import com.example.ui.SlotDisplayItem
import com.example.ui.SubjectAttendanceStats
import com.example.ui.components.GlassCard
import com.example.ui.theme.CaliperTheme
import kotlinx.coroutines.launch

@Composable
fun AiAssistantDialog(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors
    val coroutineScope = rememberCoroutineScope()

    val statsList by viewModel.subjectStats.collectAsState()
    val todaySlots by viewModel.todaySlots.collectAsState()
    val assessments by viewModel.assessments.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val notes by viewModel.notes.collectAsState()

    var userQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Instant local analysis strategy
    val strategy = remember(statsList, todaySlots, assessments, tasks) {
        CaliperAiEngine.analyzeDailyStrategy(statsList, todaySlots, assessments, tasks)
    }

    var conversationHistory by remember {
        mutableStateOf(
            listOf(
                Pair(
                    false, // isUser
                    "Hello Engineer! I'm Caliper AI, your academic & bunk strategist.\n\n" +
                            "• ${strategy.headline}\n" +
                            "• ${strategy.pendingTasksSummary}\n\n" +
                            "Tap any prompt below or ask me about your lectures, drawing sheets, or safe bunks!"
                )
            )
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f),
                borderColors = listOf(colors.primaryAccent.copy(alpha = 0.6f), colors.cardBorderBottom)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
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
                                    .clip(CircleShape)
                                    .background(colors.primaryAccent.copy(alpha = 0.15f))
                                    .border(1.dp, colors.primaryAccent.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Assistant",
                                    tint = colors.primaryAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Caliper Academic AI",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = strategy.overallStatusBadge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (strategy.overallStatusBadge.contains("RISK")) colors.bunkDanger else colors.safeZone
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                CaliperSoundManager.playSnap()
                                onDismissRequest()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick AI Shortcut Action Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionChip("Can I Bunk?", Icons.Default.DirectionsRun) {
                            sendQuery("Can I bunk my classes today?", coroutineScope, viewModel, statsList, todaySlots, assessments, tasks, notes, conversationHistory) {
                                conversationHistory = it
                            }
                        }
                        QuickActionChip("Deadlines", Icons.Default.AssignmentLate) {
                            sendQuery("What drawing sheets and tasks are pending?", coroutineScope, viewModel, statsList, todaySlots, assessments, tasks, notes, conversationHistory) {
                                conversationHistory = it
                            }
                        }
                        QuickActionChip("Today's Plan", Icons.Default.Checklist) {
                            sendQuery("Give me my daily action plan for today", coroutineScope, viewModel, statsList, todaySlots, assessments, tasks, notes, conversationHistory) {
                                conversationHistory = it
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Conversation / Insights Stream
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Urgent Deadlines Banner if any
                        if (strategy.urgentDeadlines.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.deadlineWarning.copy(alpha = 0.12f))
                                        .border(0.5.dp, colors.deadlineWarning.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = colors.deadlineWarning, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Urgent Deadlines Watchdog",
                                                color = colors.deadlineWarning,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                        strategy.urgentDeadlines.forEach { d ->
                                            Text(
                                                text = "• ${d.title} (${d.type}) — Due: ${d.dueDate}",
                                                color = colors.textPrimary,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Chat bubbles
                        items(conversationHistory) { (isUser, text) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 280.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isUser) colors.primaryAccent.copy(alpha = 0.22f)
                                            else Color.White.copy(alpha = 0.05f)
                                        )
                                        .border(
                                            0.5.dp,
                                            if (isUser) colors.primaryAccent.copy(alpha = 0.5f)
                                            else Color.White.copy(alpha = 0.10f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = text,
                                        color = colors.textPrimary,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }

                        if (isLoading) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = colors.primaryAccent,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Caliper AI analyzing telemetry...",
                                        fontSize = 11.sp,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search / Query Input Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = userQuery,
                            onValueChange = { userQuery = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 13.sp
                            ),
                            cursorBrush = SolidColor(colors.primaryAccent),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (userQuery.isEmpty()) {
                                    Text(
                                        text = "Ask Caliper AI anything...",
                                        color = colors.textMuted,
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        IconButton(
                            onClick = {
                                if (userQuery.isNotBlank() && !isLoading) {
                                    val q = userQuery.trim()
                                    userQuery = ""
                                    CaliperSoundManager.playSnap()
                                    CaliperHapticManager.tick(context)
                                    isLoading = true
                                    val updated = conversationHistory + Pair(true, q)
                                    conversationHistory = updated

                                    coroutineScope.launch {
                                        val reply = CaliperAiEngine.queryAssistant(
                                            prompt = q,
                                            statsList = statsList,
                                            slots = todaySlots,
                                            assessments = assessments,
                                            tasks = tasks,
                                            notes = notes
                                        )
                                        conversationHistory = updated + Pair(false, reply)
                                        isLoading = false
                                        CaliperSoundManager.playSuccess()
                                    }
                                }
                            },
                            enabled = userQuery.isNotBlank() && !isLoading,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (userQuery.isNotBlank()) colors.primaryAccent else colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun sendQuery(
    prompt: String,
    scope: kotlinx.coroutines.CoroutineScope,
    viewModel: MainViewModel,
    statsList: List<SubjectAttendanceStats>,
    todaySlots: List<com.example.ui.SlotDisplayItem>,
    assessments: List<com.example.data.local.entity.AssessmentEntity>,
    tasks: List<com.example.data.local.entity.AcademicTaskEntity>,
    notes: List<com.example.data.local.entity.QuickNoteEntity>,
    currentHistory: List<Pair<Boolean, String>>,
    onResult: (List<Pair<Boolean, String>>) -> Unit
) {
    CaliperSoundManager.playSnap()
    val updated = currentHistory + Pair(true, prompt)
    onResult(updated)

    scope.launch {
        val reply = CaliperAiEngine.queryAssistant(
            prompt = prompt,
            statsList = statsList,
            slots = todaySlots,
            assessments = assessments,
            tasks = tasks,
            notes = notes
        )
        onResult(updated + Pair(false, reply))
        CaliperSoundManager.playSuccess()
    }
}

@Composable
private fun RowScope.QuickActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val colors = CaliperTheme.colors
    Row(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
