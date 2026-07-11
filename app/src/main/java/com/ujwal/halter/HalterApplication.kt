// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter

import android.app.Application
import com.ujwal.halter.data.HalterDatabase
import com.ujwal.halter.data.HalterRepository
import com.ujwal.halter.service.BlockDecisionEngine
import com.ujwal.halter.service.HoverOverlay
import com.ujwal.halter.service.OverlayController
import com.ujwal.halter.service.ScrollDetector
import com.ujwal.halter.service.SystemClock
import com.ujwal.halter.service.scheduleHalterWorkers
import com.ujwal.halter.settings.SettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class HalterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@HalterApplication)
            modules(appModule)
        }
        scheduleHalterWorkers(this)
    }
}

private val appModule = module {
    single { HalterDatabase.create(get()) }
    single { get<HalterDatabase>().monitoredAppDao() }
    single { get<HalterDatabase>().usageSessionDao() }
    single { get<HalterDatabase>().scrollEventDao() }
    single { get<HalterDatabase>().blockScheduleDao() }
    single { get<HalterDatabase>().focusSessionDao() }
    single { get<HalterDatabase>().journalDao() }
    single { get<HalterDatabase>().routineDao() }
    single { SettingsRepository(get()) }
    single { HalterRepository(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { BlockDecisionEngine(get(), get(), androidContext().packageName) }
    single { ScrollDetector(get(), SystemClock) }
    single { OverlayController(get()) }
    single { HoverOverlay(get()) }
}
