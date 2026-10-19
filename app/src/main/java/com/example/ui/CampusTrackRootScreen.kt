package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.navigation.AppTab
import com.example.ui.theme.CaliperTheme
import com.example.ui.theme.SpaceGroteskFamily

@Composable
fun CampusTrackRootScreen(viewModel: MainViewModel) {
    MainScaffold(viewModel = viewModel)
}

@Composable
fun CampusHeader(
    currentTab: AppTab,
    onOpenSettings: () -> Unit = {}
) {
    val colors = CaliperTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currentTab.title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        )
    }
}

@Composable
fun FloatingGlassNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    CompactFloatingGlassNavBar(
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        modifier = modifier
    )
}
