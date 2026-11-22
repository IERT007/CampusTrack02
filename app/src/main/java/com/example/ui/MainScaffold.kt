package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import com.example.audio.CaliperHapticManager
import com.example.audio.CaliperSoundManager
import com.example.ui.components.MainTopBar
import com.example.ui.dialogs.OtaUpdateDialog
import com.example.ui.navigation.AppTab
import com.example.ui.screens.audit.AuditScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.subjects.SafeZoneScreen
import com.example.ui.screens.today.TodayScreen
import com.example.ui.theme.CaliperTheme

/**
 * MainScaffold — CALIPER v2.0 3-Tab Container
 * Centered ultra-compact floating bottom pill (216.dp x 50.dp) with spring sliding indicator.
 */
@Composable
fun MainScaffold(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
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

    BackHandler(enabled = currentTab != AppTab.TODAY) {
        currentTab = AppTab.TODAY
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colors.background,
                                colors.surface,
                                colors.background
                            )
                        )
                    )
            )

            val activeSemester by viewModel.activeSemester.collectAsState()
            val isArchived by viewModel.isActiveSemesterArchived.collectAsState()

            Column(modifier = Modifier.fillMaxSize()) {
                MainTopBar(
                    viewModel = viewModel,
                    onNavigateToSettings = {
                        CaliperSoundManager.playSnap(view)
                        CaliperHapticManager.tick(context)
                        showSettingsScreen = true
                    }
                )

                if (isArchived) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.warnAmber.copy(alpha = 0.14f))
                            .border(0.5.dp, colors.warnAmber.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                            .clickable { showSettingsScreen = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("archived_readonly_banner"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Archived read-only",
                                tint = colors.warnAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${activeSemester?.name ?: "Semester"} • ARCHIVED (READ-ONLY)",
                                color = colors.warnAmber,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "SWITCH",
                            color = colors.primaryAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

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

            CompactFloatingGlassNavBar(
                selectedTab = currentTab,
                onTabSelected = { tab ->
                    if (tab != currentTab) {
                        CaliperSoundManager.playSnap(view)
                        CaliperHapticManager.tick(context)
                        currentTab = tab
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp)
            )
        }
    }

    OtaUpdateDialog(viewModel = viewModel)
}

/**
 * Ultra-Compact Centered Floating Bottom Pill Bar (216.dp x 50.dp)
 * Features a single 68.dp translucent sliding indicator pill driven by spring physics.
 */
@Composable
fun CompactFloatingGlassNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CaliperTheme.colors
    val tabs = AppTab.entries
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    // Pill outer width = 216.dp; horizontal padding = 6.dp each side -> 204.dp track -> 3 slots of 68.dp each
    val indicatorWidth = 68.dp
    val indicatorOffset by animateDpAsState(
        targetValue = indicatorWidth * selectedIndex,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 400f
        ),
        label = "floating_nav_pill_offset"
    )

    Box(
        modifier = modifier
            .width(216.dp)
            .height(50.dp)
            .clip(CircleShape)
            .background(colors.surfaceVariant.copy(alpha = 0.90f))
            .border(0.5.dp, colors.border, CircleShape)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("floating_glass_nav_bar"),
        contentAlignment = Alignment.CenterStart
    ) {
        // Fluid Sliding Translucent Indicator Pill
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(indicatorWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(colors.primaryAccent.copy(alpha = 0.16f))
                .border(0.5.dp, colors.primaryAccent.copy(alpha = 0.35f), CircleShape)
        )

        // 3 Equal-Width Navigation Tab Targets
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = (selectedTab == tab)
                val itemTint by animateColorAsState(
                    targetValue = if (isSelected) colors.primaryAccent else colors.textSecondary,
                    label = "nav_item_tint"
                )

                Box(
                    modifier = Modifier
                        .width(indicatorWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
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
                            tint = itemTint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = tab.title,
                            fontSize = 9.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = itemTint,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
