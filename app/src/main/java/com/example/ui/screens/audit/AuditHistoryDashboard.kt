package com.example.ui.screens.audit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.MainViewModel

/**
 * Backward compatibility delegation to AuditScreen
 */
@Composable
fun AuditHistoryDashboard(
    viewModel: MainViewModel,
    onNavigateToDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AuditScreen(
        viewModel = viewModel,
        onNavigateToDate = onNavigateToDate,
        modifier = modifier
    )
}
