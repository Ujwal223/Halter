// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ujwal.halter.data.FocusSession
import com.ujwal.halter.data.HalterRepository
import com.ujwal.halter.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class FocusTileService : TileService() {
    private val repository: HalterRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onClick() {
        super.onClick()
        scope.launch {
            val active = repository.activeFocusSession()
            if (active == null) {
                val settings = settingsRepository.settings.first()
                repository.saveFocusSession(
                    FocusSession(
                        startEpochMillis = System.currentTimeMillis(),
                        durationMinutes = settings.defaultFocusSessionMinutes,
                        completed = false
                    )
                )
            } else {
                repository.saveFocusSession(active.copy(completed = true))
            }
            updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { updateTile() }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun updateTile() {
        qsTile?.let {
            it.state = if (repository.activeFocusSession() == null) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            it.label = "Quick Focus"
            it.updateTile()
        }
    }
}
