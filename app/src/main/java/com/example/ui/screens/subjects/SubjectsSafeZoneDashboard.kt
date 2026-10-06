package com.example.ui.screens.subjects

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.MainViewModel

/**
 * Backward-compatible delegation to SafeZoneScreen
 */
@Composable
fun SubjectsSafeZoneDashboard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    SafeZoneScreen(
        viewModel = viewModel,
        modifier = modifier
    )
}
