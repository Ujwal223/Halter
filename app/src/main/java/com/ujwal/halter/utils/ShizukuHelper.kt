// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import rikka.shizuku.Shizuku

object ShizukuHelper {

    fun hasWriteSecureSettings(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    fun requestPermissionAndGrant(activity: Activity, onComplete: (Boolean) -> Unit) {
        if (hasWriteSecureSettings(activity)) {
            onComplete(true)
            return
        }

        if (!isShizukuRunning()) {
            Toast.makeText(activity, "Shizuku is not running. Please start Shizuku first.", Toast.LENGTH_LONG).show()
            onComplete(false)
            return
        }

        try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                val success = executeGrant(activity)
                onComplete(success)
            } else {
                // Register a listener for permission result
                val listener = object : Shizuku.OnRequestPermissionResultListener {
                    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                        if (requestCode == 1002) {
                            Shizuku.removeRequestPermissionResultListener(this)
                            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                                val success = executeGrant(activity)
                                onComplete(success)
                            } else {
                                Toast.makeText(activity, "Shizuku permission denied.", Toast.LENGTH_SHORT).show()
                                onComplete(false)
                            }
                        }
                    }
                }
                Shizuku.addRequestPermissionResultListener(listener)
                Shizuku.requestPermission(1002)
            }
        } catch (e: Throwable) {
            Toast.makeText(activity, "Shizuku error: ${e.message}", Toast.LENGTH_LONG).show()
            onComplete(false)
        }
    }

    private fun executeGrant(context: Context): Boolean {
        return try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(
                null,
                arrayOf("pm", "grant", context.packageName, "android.permission.WRITE_SECURE_SETTINGS"),
                null,
                null
            ) as java.lang.Process
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                Toast.makeText(context, "WRITE_SECURE_SETTINGS granted successfully!", Toast.LENGTH_LONG).show()
                true
            } else {
                Toast.makeText(context, "Failed to grant: exit code $exitCode", Toast.LENGTH_LONG).show()
                false
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Shizuku execution failed: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    fun launchShizuku(context: Context) {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        if (intent != null) {
            context.startActivity(intent)
        } else {
            val playStoreIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=moe.shizuku.privileged.api")
            )
            try {
                context.startActivity(playStoreIntent)
            } catch (_: Exception) {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://shizuku.rikka.app/")
                    )
                )
            }
        }
    }
}
