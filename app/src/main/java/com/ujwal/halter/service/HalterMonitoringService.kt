// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

class HalterMonitoringService : Service() {
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Halter protection", NotificationManager.IMPORTANCE_LOW)
        )
        startForeground(7, notification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Halter is protecting you")
            .setContentText("Local app and scroll limits are active.")
            .setSmallIcon(com.ujwal.halter.R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private companion object {
        const val CHANNEL_ID = "halter_monitoring"
    }
}
