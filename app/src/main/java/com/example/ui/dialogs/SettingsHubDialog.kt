package com.example.ui.dialogs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.MainViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.CaliperTheme

@Composable
fun SettingsHubDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CaliperTheme.colors.baseSurface
        ) {
            SettingsScreen(
                viewModel = viewModel,
                onClose = onDismiss
            )
        }
    }
}
