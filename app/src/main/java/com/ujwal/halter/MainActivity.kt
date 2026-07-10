// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.ujwal.halter.settings.SettingsRepository
import com.ujwal.halter.ui.HalterApp
import com.ujwal.halter.ui.theme.HalterTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val settingsRepository: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Fix status bar appearance for Samsung OneUI
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Initialize first-install timestamp (no-op if already set)
        CoroutineScope(Dispatchers.IO).launch {
            settingsRepository.firstInstallEpochMillis()
        }
        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = com.ujwal.halter.settings.HalterSettings())
            HalterTheme(settings = settings) {
                HalterApp()
            }
        }
    }
}
