package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * CALIPER OS — Streamlined 3-Tab Primary Navigation Engine
 * 1. Today — Daily schedule, in-slot logging & class test entry, 1-day custom activities.
 * 2. Safe-Zone — Statutory 75% telemetry, bunk buffer simulator, curriculum subject management & deletion.
 * 3. Audit — Interactive monthly calendar, progression trajectory wave, and academic test performance ledger.
 */
enum class AppTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    TODAY(
        title = "Today",
        selectedIcon = Icons.Default.Today,
        unselectedIcon = Icons.Default.CalendarToday,
        testTag = "nav_tab_today"
    ),
    SAFE_ZONE(
        title = "Safe-Zone",
        selectedIcon = Icons.Default.Shield,
        unselectedIcon = Icons.Default.Security,
        testTag = "nav_tab_safe_zone"
    ),
    AUDIT(
        title = "Audit",
        selectedIcon = Icons.Default.Analytics,
        unselectedIcon = Icons.Default.Insights,
        testTag = "nav_tab_audit"
    )
}
