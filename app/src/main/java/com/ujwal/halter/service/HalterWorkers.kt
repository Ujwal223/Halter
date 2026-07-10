// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class DailyLimitWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = Result.success()
}

class WeeklyReportWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = Result.success()
}

fun scheduleHalterWorkers(context: Context) {
    val workManager = WorkManager.getInstance(context)
    workManager.enqueueUniquePeriodicWork(
        "daily_limit_reset",
        ExistingPeriodicWorkPolicy.UPDATE,
        PeriodicWorkRequestBuilder<DailyLimitWorker>(1, TimeUnit.DAYS).build()
    )
    workManager.enqueueUniquePeriodicWork(
        "weekly_honesty_report",
        ExistingPeriodicWorkPolicy.UPDATE,
        PeriodicWorkRequestBuilder<WeeklyReportWorker>(7, TimeUnit.DAYS).build()
    )
}
