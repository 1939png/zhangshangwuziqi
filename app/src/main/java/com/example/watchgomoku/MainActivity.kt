package com.example.watchgomoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.watchgomoku.navigation.GomokuNavHost
import com.example.watchgomoku.presentation.settings.SettingsViewModel
import com.example.watchgomoku.ui.theme.GomokuTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GomokuApp()
        }
    }
}

@Composable
fun GomokuApp() {
    val context = LocalContext.current
    val app = context.applicationContext as GomokuApplication
    val settingsViewModel: SettingsViewModel = viewModel {
        SettingsViewModel(app.container)
    }
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    GomokuTheme(
        themeMode = settings.themeMode,
        highContrast = settings.highContrast,
        fontScale = settings.fontScale,
        colorTheme = settings.colorTheme
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            GomokuNavHost(
                navController = navController,
                settingsViewModel = settingsViewModel
            )
        }
    }
}
