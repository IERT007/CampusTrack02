package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.ui.components.MainTopBar
import com.example.ui.navigation.AppTab
import com.example.ui.screens.audit.AuditScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.subjects.SafeZoneScreen
import com.example.ui.screens.today.TodayScreen
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.CaliperTheme

/**
 * MainScaffold — CALIPER OS 3-Tab Container
 * Exactly 3 primary navigation tabs:
 * 1. Today — Daily schedule, in-slot logging, class test entry, 1-day custom activities.
 * 2. Safe-Zone — Attendance telemetry, bunk simulator, subject management & cascading delete.
 * 3. Audit — Interactive monthly calendar, progression wave, academic test performance ledger.
 */
@Composable
fun MainScaffold(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = CaliperTheme.colors

    var currentTab by remember { mutableStateOf(AppTab.TODAY) }
    var showSettingsScreen by remember { mutableStateOf(false) }

    if (showSettingsScreen) {
        SettingsScreen(
            viewModel = viewModel,
            onNavigateBack = { showSettingsScreen = false }
        )
        return
    }

    // Android back button: If not on TODAY, return to TODAY
    BackHandler(enabled = currentTab != AppTab.TODAY) {
        currentTab = AppTab.TODAY
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
                    }
                )

                // Dedicated View Containers with animated transitions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (currentTab) {
                        AppTab.TODAY -> {
                            TodayScreen(viewModel = viewModel)
                        }
                        AppTab.SAFE_ZONE -> {
                            SafeZoneScreen(viewModel = viewModel)
                        }
                        AppTab.AUDIT -> {
                            AuditScreen(
                                viewModel = viewModel,
                                onNavigateToDate = { dateStr ->
                                    viewModel.setSelectedDate(dateStr)
                                    currentTab = AppTab.TODAY
                                }
                            )
                        }
                    }
                }
            }

            // Floating Frosted Glass Bottom Navigation Bar (Exactly 3 Tabs)
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
        }
    }
}
