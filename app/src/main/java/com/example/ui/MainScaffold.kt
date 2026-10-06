package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.ui.components.CaliperRoboCompanion
import com.example.ui.components.GlassSurface
import com.example.ui.components.MainTopBar
import com.example.ui.dialogs.AiAssistantDialog
import com.example.ui.screens.academics.AcademicsDashboard
import com.example.ui.screens.audit.AuditScreen
import com.example.ui.screens.notes.QuickNotesScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.subjects.SafeZoneScreen
import com.example.ui.screens.today.TodayScreen
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.GlassBorderBottom
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted

/**
 * MainScaffold — Unified Caliper OS Application Container
 * Binds:
 * 1. Centered Google-style frosted glass search bar in MainTopBar with reactive autocomplete & telemetry.
 * 2. Multi-dashboard navigation: Today, Safe-Zone, Academics, Notes & AI Vault, Audit.
 * 3. AI Academic Copilot dialog for bunk calculations, task reminders, and deadline alerts.
 * 4. Dedicated full-page SettingsScreen navigation.
 */
@Composable
fun MainScaffold(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors

    var currentTab by remember { mutableStateOf(DashboardTab.TODAY) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showAiAssistantDialog by remember { mutableStateOf(false) }

    if (showSettingsScreen) {
        SettingsScreen(
            viewModel = viewModel,
            onNavigateBack = { showSettingsScreen = false }
        )
        return
    }

    // Android back button: If not on TODAY, return to TODAY
    BackHandler(enabled = currentTab != DashboardTab.TODAY) {
        currentTab = DashboardTab.TODAY
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AmoledBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Ambient radial background glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colors.baseSurface,
                                colors.containerSurface,
                                colors.baseSurface
                            )
                        )
                    )
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // Unified Google-Style Frosted Glass Top Bar
                MainTopBar(
                    viewModel = viewModel,
                    onNavigateToSettings = {
                        CaliperSoundManager.playSnap()
                        CaliperHapticManager.tick(context)
                        showSettingsScreen = true
                    },
                    onOpenAiAssistant = {
                        CaliperSoundManager.playSnap()
                        CaliperHapticManager.tick(context)
                        showAiAssistantDialog = true
                    }
                )

                // Dedicated View Containers with animated transitions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (currentTab) {
                        DashboardTab.TODAY -> {
                            TodayScreen(viewModel = viewModel)
                        }
                        DashboardTab.SAFE_ZONE -> {
                            SafeZoneScreen(viewModel = viewModel)
                        }
                        DashboardTab.ACADEMICS -> {
                            AcademicsDashboard(viewModel = viewModel)
                        }
                        DashboardTab.NOTES -> {
                            QuickNotesScreen(viewModel = viewModel)
                        }
                        DashboardTab.AUDIT -> {
                            AuditScreen(
                                viewModel = viewModel,
                                onNavigateToDate = { dateStr ->
                                    viewModel.setSelectedDate(dateStr)
                                    currentTab = DashboardTab.TODAY
                                }
                            )
                        }
                    }
                }
            }

            // Floating Frosted Glass Bottom Navigation Bar
            FloatingGlassNavBar(
                selectedTab = currentTab,
                onTabSelected = { tab ->
                    CaliperSoundManager.playSnap()
                    CaliperHapticManager.tick(context)
                    currentTab = tab
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            )

            // Interactive 3D Floating Robo Copilot with drag & mood physics
            CaliperRoboCompanion(viewModel = viewModel)

            // AI Assistant Copilot Modal Dialog
            if (showAiAssistantDialog) {
                AiAssistantDialog(
                    viewModel = viewModel,
                    onDismissRequest = { showAiAssistantDialog = false }
                )
            }
        }
    }
}
