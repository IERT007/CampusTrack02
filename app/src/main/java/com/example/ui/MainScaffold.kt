package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHapticManager
import com.example.data.local.entity.AcademicTaskEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.MainTopBar
import com.example.ui.dialogs.SettingsHubDialog
import com.example.ui.navigation.AppTab
import com.example.ui.screens.audit.AuditScreen
import com.example.ui.screens.subjects.SafeZoneScreen
import com.example.ui.screens.today.TodayScreen
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.PlusJakartaFamily
import com.example.ui.theme.fluidTouchFeedback
import java.time.LocalDate

@Composable
fun MainScaffold(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val colors = CaliperTheme.colors
    var currentTab by rememberSaveable { mutableStateOf(AppTab.TODAY) }
    var showSettingsModal by rememberSaveable { mutableStateOf(false) }
    var showQuickAddModal by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = currentTab != AppTab.TODAY) {
        currentTab = AppTab.TODAY
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.baseSurface,
                        colors.baseSurfaceSecondary,
                        colors.baseSurface
                    )
                )
            )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                MainTopBar(
                    viewModel = viewModel,
                    onOpenSettings = { showSettingsModal = true }
                )
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                },
                label = "main_tab_transition",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { tab ->
                when (tab) {
                    AppTab.TODAY -> TodayScreen(viewModel = viewModel)
                    AppTab.SAFE_ZONE -> SafeZoneScreen(viewModel = viewModel)
                    AppTab.AUDIT -> AuditScreen(
                        viewModel = viewModel,
                        onJumpToDate = { currentTab = AppTab.TODAY }
                    )
                }
            }
        }

        CompactFloatingGlassNavBar(
            selectedTab = currentTab,
            onTabSelected = { currentTab = it },
            onQuickCreateClick = { showQuickAddModal = true },
            onOpenProfileSettings = { showSettingsModal = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp, start = 20.dp, end = 20.dp)
        )
    }

    if (showSettingsModal) {
        SettingsHubDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsModal = false }
        )
    }

    if (showQuickAddModal) {
        QuickCreateProductivityModal(
            viewModel = viewModel,
            onDismiss = { showQuickAddModal = false }
        )
    }
}

@Composable
fun CompactFloatingGlassNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onQuickCreateClick: () -> Unit = {},
    onOpenProfileSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors
    val dockShape = RoundedCornerShape(32.dp)
    val shadowColor = if (colors.isLightGlass) {
        Color(0xFF4A4238).copy(alpha = 0.14f)
    } else {
        Color.Black.copy(alpha = 0.50f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .shadow(12.dp, dockShape, ambientColor = shadowColor, spotColor = shadowColor)
            .clip(dockShape)
            .background(colors.containerSurface, dockShape)
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(colors.cardBorderTop, colors.cardBorderBottom)
                ),
                shape = dockShape
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Today (Home)
        val isToday = selectedTab == AppTab.TODAY
        NavDockItem(
            icon = if (isToday) Icons.Filled.Home else Icons.Outlined.Home,
            label = "Today",
            isSelected = isToday,
            testTag = AppTab.TODAY.testTag,
            onClick = {
                CaliperHapticManager.tick(context)
                onTabSelected(AppTab.TODAY)
            }
        )

        // 2. Subjects / Safe Zone (Grid)
        val isSafeZone = selectedTab == AppTab.SAFE_ZONE
        NavDockItem(
            icon = Icons.Outlined.GridView,
            label = "Subjects",
            isSelected = isSafeZone,
            testTag = AppTab.SAFE_ZONE.testTag,
            onClick = {
                CaliperHapticManager.tick(context)
                onTabSelected(AppTab.SAFE_ZONE)
            }
        )

        // 3. Central Circular Warm Taupe '+' Button (From reference design)
        Box(
            modifier = Modifier
                .size(48.dp)
                .fluidTouchFeedback(
                    enabled = true,
                    onClick = {
                        CaliperHapticManager.confirmTap(context)
                        onQuickCreateClick()
                    }
                )
                .clip(CircleShape)
                .background(colors.scheduledLecture, CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                .testTag("nav_quick_create_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Quick Create Task or Note",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        // 4. Statistics / Audit (Chart)
        val isAudit = selectedTab == AppTab.AUDIT
        NavDockItem(
            icon = Icons.Outlined.BarChart,
            label = "Stats",
            isSelected = isAudit,
            testTag = AppTab.AUDIT.testTag,
            onClick = {
                CaliperHapticManager.tick(context)
                onTabSelected(AppTab.AUDIT)
            }
        )

        // 5. Profile / Settings
        NavDockItem(
            icon = Icons.Outlined.PersonOutline,
            label = "Setup",
            isSelected = false,
            testTag = "nav_tab_profile_settings",
            onClick = {
                CaliperHapticManager.tick(context)
                onOpenProfileSettings()
            }
        )
    }
}

@Composable
private fun NavDockItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val colors = CaliperTheme.colors
    val pillShape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .size(width = 56.dp, height = 48.dp)
            .fluidTouchFeedback(enabled = true, onClick = onClick)
            .clip(pillShape)
            .background(
                if (isSelected) colors.scheduledLecture.copy(alpha = 0.16f) else Color.Transparent,
                pillShape
            )
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) colors.textPrimary else colors.textSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = PlusJakartaFamily,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 10.sp,
                color = if (isSelected) colors.textPrimary else colors.textSecondary
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCreateProductivityModal(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val colors = CaliperTheme.colors
    val subjects by viewModel.subjects.collectAsState()
    var mode by remember { mutableStateOf("TASK") } // TASK, NOTE, EXTRA
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var selectedSubjectCode by remember { mutableStateOf(subjects.firstOrNull()?.code ?: "GENERAL") }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = "Quick Productivity Entry",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Capture tasks, study notes, or extra classes in one tap",
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("TASK" to "Study Task", "NOTE" to "Quick Note", "EXTRA" to "Extra Class").forEach { (key, label) ->
                        FilterChip(
                            selected = mode == key,
                            onClick = { mode = key },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = {
                        Text(
                            when (mode) {
                                "TASK" -> "Task Title (e.g., Complete Lab File)"
                                "NOTE" -> "Note Title (e.g., Exam Formula)"
                                else -> "Extra Class Topic / Note"
                            }
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (mode != "EXTRA") {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = {
                            Text(
                                if (mode == "TASK") "Due Time / Priority (e.g., 05:00 PM)"
                                else "Details / Key Points"
                            )
                        },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.scheduledLecture)
                            .fluidTouchFeedback(
                                onClick = {
                                    when (mode) {
                                        "TASK" -> {
                                            if (title.isNotBlank()) {
                                                viewModel.addTask(
                                                    AcademicTaskEntity(
                                                        description = title.trim(),
                                                        category = selectedSubjectCode,
                                                        dueDate = LocalDate.now().toString(),
                                                        priority = subtitle.ifBlank { "Today" },
                                                        isCompleted = false
                                                    )
                                                )
                                                onDismiss()
                                            }
                                        }
                                        "NOTE" -> {
                                            if (title.isNotBlank()) {
                                                viewModel.addNote(
                                                    title = title.trim(),
                                                    content = subtitle.trim().ifBlank { "Saved study note" },
                                                    subjectCode = selectedSubjectCode,
                                                    colorHex = "#8B7D6B"
                                                )
                                                onDismiss()
                                            }
                                        }
                                        "EXTRA" -> {
                                            val subjId = subjects.firstOrNull()?.id ?: 0L
                                            if (subjId != 0L) {
                                                viewModel.logExtraClass(
                                                    subjectId = subjId,
                                                    status = "PRESENT",
                                                    isProxy = false,
                                                    notes = title.ifBlank { "Extra Class" }
                                                )
                                                onDismiss()
                                            }
                                        }
                                    }
                                }
                            )
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}
