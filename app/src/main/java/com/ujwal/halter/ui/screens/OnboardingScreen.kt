// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ujwal.halter.HalterPermissionState
import com.ujwal.halter.SpecialPermission
import com.ujwal.halter.halterPermissionState
import com.ujwal.halter.specialPermissionIntent

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionState by remember { mutableStateOf(context.halterPermissionState()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionState = context.halterPermissionState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ScreenColumn(title = "Halter") {
        Text("Set up the three Android permissions Halter needs to enforce limits locally.", style = MaterialTheme.typography.bodyLarge)
        PermissionCard(
            title = "Accessibility",
            body = "Detects foreground apps and short-video scroll events.",
            granted = permissionState.accessibilityEnabled,
            onOpen = { context.startActivity(context.specialPermissionIntent(SpecialPermission.ACCESSIBILITY)) }
        )
        PermissionCard(
            title = "Usage Access",
            body = "Reads local screen time so daily and session limits can be enforced.",
            granted = permissionState.usageAccessEnabled,
            onOpen = { context.startActivity(context.specialPermissionIntent(SpecialPermission.USAGE_ACCESS)) }
        )
        PermissionCard(
            title = "Draw Over Apps",
            body = "Shows breathing, hold-to-open, and block overlays above distracting apps.",
            granted = permissionState.overlayEnabled,
            onOpen = { context.startActivity(context.specialPermissionIntent(SpecialPermission.OVERLAY)) }
        )
        Button(onClick = onContinue, enabled = permissionState.allSpecialPermissionsGranted, modifier = Modifier.fillMaxWidth()) {
            Text(if (permissionState.allSpecialPermissionsGranted) "Choose Apps" else "Grant permissions to continue")
        }
    }
}

@Composable
private fun PermissionCard(title: String, body: String, granted: Boolean, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(if (granted) Icons.Outlined.CheckCircle else Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onOpen,
                enabled = !granted
            ) { Text(if (granted) "Granted" else "Enable") }
        }
    }
}
