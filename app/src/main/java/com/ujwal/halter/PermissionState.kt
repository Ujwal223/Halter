// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.getSystemService
import com.ujwal.halter.utils.ShizukuHelper

data class HalterPermissionState(
    val accessibilityEnabled: Boolean,
    val usageAccessEnabled: Boolean,
    val overlayEnabled: Boolean,
    val secureSettingsEnabled: Boolean
) {
    val allSpecialPermissionsGranted: Boolean =
        accessibilityEnabled && usageAccessEnabled && overlayEnabled
}

fun Context.halterPermissionState(): HalterPermissionState = HalterPermissionState(
    accessibilityEnabled = isHalterAccessibilityEnabled(),
    usageAccessEnabled = hasUsageAccess(),
    overlayEnabled = Settings.canDrawOverlays(this),
    secureSettingsEnabled = ShizukuHelper.hasWriteSecureSettings(this)
)

fun Context.specialPermissionIntent(kind: SpecialPermission): Intent = when (kind) {
    SpecialPermission.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    SpecialPermission.USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    SpecialPermission.OVERLAY -> Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:$packageName")
    )
    SpecialPermission.RESTRICTED_SETTINGS -> Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:$packageName")
    )
}.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

enum class SpecialPermission { ACCESSIBILITY, USAGE_ACCESS, OVERLAY, RESTRICTED_SETTINGS }

private fun Context.isHalterAccessibilityEnabled(): Boolean {
    val manager = getSystemService<AccessibilityManager>() ?: return false
    val serviceId = "$packageName/${com.ujwal.halter.service.HalterAccessibilityService::class.java.name}"
    return manager.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { it.resolveInfo.serviceInfo.run { "$packageName/$name" } == serviceId }
}

private fun Context.hasUsageAccess(): Boolean {
    val appOps = getSystemService<AppOpsManager>() ?: return false
    @Suppress("DEPRECATION")
    val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}
