package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.audio.CaliperHardwareEngine
import com.example.notification.NotificationHelper
import com.example.ui.CampusTrackRootScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CaliperHardwareEngine.enableHighRefreshRate(this)
        NotificationHelper.createNotificationChannels(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CampusTrackRootScreen(viewModel = viewModel)
            }
        }
    }
}
