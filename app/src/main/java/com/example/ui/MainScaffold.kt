package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * Ultra-slim, ergonomic navigation container:
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

            // Compact 54.dp Frosted Glass Bottom Navigation Bar (Ultra-Slim)
            CompactFloatingGlassNavBar(
                selectedTab = currentTab,
                onTabSelected = { tab ->
                    CaliperSoundManager.playSnap()
                    CaliperHapticManager.tick(context)
                    currentTab = tab
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Compact 54.dp 3-Tab Bottom Navigation Bar
 * - Container height: 54.dp
 * - Internal padding: icon-to-label spacing 2.dp, icon size 20.dp
 * - Tab labels: 11.sp Medium typography
 * - Selected indicator: subtle translucent pill (Color.White.copy(alpha = 0.08f))
 * - Styling: Translucent frosted obsidian (Color(0xFF0A0D14).copy(alpha = 0.85f)) with 0.5.dp border
 */
@Composable
fun CompactFloatingGlassNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF0A0D14).copy(alpha = 0.85f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .testTag("floating_glass_nav_bar"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTab.values().forEach { tab ->
                val isSelected = (selectedTab == tab)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = Color.White.copy(alpha = 0.2f)),
                            onClick = { onTabSelected(tab) }
                        )
                        .testTag(tab.testTag),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.title,
                            tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}
