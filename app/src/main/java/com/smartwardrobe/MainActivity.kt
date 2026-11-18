package com.smartwardrobe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import com.smartwardrobe.ui.navigation.SmartWardrobeApp
import com.smartwardrobe.ui.settings.SettingsViewModel
import com.smartwardrobe.ui.theme.SmartWardrobeTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val settingsVm: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsVm.darkMode.collectAsStateWithLifecycle()
            SmartWardrobeTheme(themeMode = themeMode) {
                SmartWardrobeApp()
            }
        }
    }
}
