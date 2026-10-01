package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ui.components.GlassSurface
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.screens.academics.AcademicsDashboard
import com.example.ui.screens.audit.AuditHistoryDashboard
import com.example.ui.screens.subjects.SubjectsSafeZoneDashboard
import com.example.ui.screens.today.TodayOperationsDashboard
import com.example.ui.theme.*

enum class DashboardTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    TODAY("Today", Icons.Default.Today, Icons.Default.CalendarToday, "nav_tab_today"),
    SAFE_ZONE("Safe-Zone", Icons.Default.Shield, Icons.Default.Security, "nav_tab_safe_zone"),
    ACADEMICS("Academics", Icons.Default.School, Icons.Default.School, "nav_tab_academics"),
    AUDIT("Audit", Icons.Default.Analytics, Icons.Default.Insights, "nav_tab_audit")
}

@Composable
fun CampusTrackRootScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(DashboardTab.TODAY) }
    var showSettingsScreen by remember { mutableStateOf(false) }

    if (showSettingsScreen) {
        com.example.ui.screens.settings.SettingsScreen(
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
        modifier = Modifier.fillMaxSize(),
        containerColor = AmoledBackground,
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val colors = com.example.ui.theme.CaliperTheme.colors

            // Background subtle ambient radial glow for liquid glassmorphism
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

            // Top Campus Header Bar (Aligned Brand Title & Translucent 20px Settings Icon)
            Column(modifier = Modifier.fillMaxSize()) {
                com.example.ui.components.MainTopBar(
                    onOpenSettings = {
                        com.example.audio.CaliperSoundManager.playSnap()
                        com.example.audio.CaliperHapticManager.tick(context)
                        showSettingsScreen = true
                    }
                )

                // 4 Dedicated Dashboard Views with smooth transitions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (currentTab) {
                        DashboardTab.TODAY -> {
                            TodayOperationsDashboard(viewModel = viewModel)
                        }
                        DashboardTab.SAFE_ZONE -> {
                            SubjectsSafeZoneDashboard(viewModel = viewModel)
                        }
                        DashboardTab.ACADEMICS -> {
                            AcademicsDashboard(viewModel = viewModel)
                        }
                        DashboardTab.AUDIT -> {
                            AuditHistoryDashboard(
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
                    com.example.audio.CaliperSoundManager.playSnap()
                    com.example.audio.CaliperHapticManager.tick(context)
                    currentTab = tab
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            )
        }
    }
}

@Composable
fun CampusHeader(
    currentTab: DashboardTab,
    onOpenSettings: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                        )
                    )
                    .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PrecisionManufacturing,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Caliper",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "IERT",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = IceBlue
                    )
                }
                Text(
                    text = "Prayagraj • Mechanical & Tool Engg",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x2238BDF8))
                    .border(0.5.dp, IceBlue.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "SEM 3 • 75% MANDATE",
                    color = IceBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x18FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings Hub",
                    tint = IceBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun FloatingGlassNavBar(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .testTag("floating_glass_nav_bar"),
        shape = RoundedCornerShape(28.dp),
        borderColors = listOf(Color(0x5500E5FF), GlassBorderBottom)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DashboardTab.values().forEach { tab ->
                val isSelected = (selectedTab == tab)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) NeonCyan.copy(alpha = 0.16f) else Color.Transparent
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = NeonCyan),
                            onClick = { onTabSelected(tab) }
                        )
                        .padding(vertical = 8.dp)
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
                            tint = if (isSelected) NeonCyan else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) NeonCyan else TextMuted
                        )
                    }
                }
            }
        }
    }
}
