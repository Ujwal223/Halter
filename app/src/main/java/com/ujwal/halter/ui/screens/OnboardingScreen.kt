// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ujwal.halter.SpecialPermission
import com.ujwal.halter.halterPermissionState
import com.ujwal.halter.specialPermissionIntent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private enum class InstallMode { AppStore, ManualApk }

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionState by remember { mutableStateOf(context.halterPermissionState()) }

    DisposableOnResume(lifecycleOwner) { permissionState = context.halterPermissionState() }

    var installMode by remember { mutableStateOf(InstallMode.AppStore) }
    var page by remember { mutableIntStateOf(0) }
    var imageIndex by remember { mutableIntStateOf(0) }

    val totalPages = 6

    Column(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // Progress row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(totalPages) { i ->
                val c = if (i <= page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                Box(Modifier.size(20.dp, 6.dp).clip(RoundedCornerShape(6.dp)).background(c))
            }
        }

        // Page content
        Card(modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Box(Modifier.fillMaxSize().padding(20.dp)) {
                when (page) {
                    0 -> WelcomePage()
                    1 -> InstallSourcePage(installMode) { installMode = it }
                    2 -> AccessibilityPage(installMode, imageIndex, onPrevImage = { if (imageIndex>0) imageIndex-- }, onNextImage = { imageIndex = (imageIndex+1)%4 }, onOpen = { context.startActivity(context.specialPermissionIntent(SpecialPermission.ACCESSIBILITY)) }, granted = permissionState.accessibilityEnabled)
                    3 -> PermissionInfoSimple(title = "Usage access", body = "Allow Halter to read screen time so daily and session limits are accurate.", onOpen = { context.startActivity(context.specialPermissionIntent(SpecialPermission.USAGE_ACCESS)) }, granted = permissionState.usageAccessEnabled)
                    4 -> PermissionInfoSimple(title = "Draw over apps", body = "Allow Halter to show overlays like breathing gates and timers.", onOpen = { context.startActivity(context.specialPermissionIntent(SpecialPermission.OVERLAY)) }, granted = permissionState.overlayEnabled)
                    else -> FinalPage()
                }
            }
        }

        // Navigation buttons (no skip)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { if (page>0) page-- }, enabled = page>0, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Back")
            }
            Button(onClick = {
                if (page < totalPages - 1) page++ else onContinue()
            }, modifier = Modifier.weight(1f)) {
                Text(if (page < totalPages - 1) "Next" else "Finish")
                Spacer(Modifier.size(8.dp))
                Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Welcome to Halter", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("This brief setup guides you through the permissions Halter needs to work reliably.", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun InstallSourcePage(current: InstallMode, onSelect: (InstallMode) -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("How did you install Halter?", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onSelect(InstallMode.AppStore) },
                modifier = Modifier.size(180.dp, 44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (current == InstallMode.AppStore) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            ) { Text("App store") }
            Button(
                onClick = { onSelect(InstallMode.ManualApk) },
                modifier = Modifier.size(180.dp, 44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (current == InstallMode.ManualApk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            ) { Text("Manual APK") }
        }
        Spacer(Modifier.height(12.dp))
        Text("If you installed via APK, some extra steps are required for restricted settings and accessibility. Choose the option that matches how you installed.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AccessibilityPage(installMode: InstallMode, imageIndex: Int, onPrevImage: () -> Unit, onNextImage: () -> Unit, onOpen: () -> Unit, granted: Boolean) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Accessibility permission", style = MaterialTheme.typography.headlineSmall)
        Text("Halter needs Accessibility to detect the current foreground app and enforce blocks.", style = MaterialTheme.typography.bodyLarge)
        if (installMode == InstallMode.ManualApk) {
            // show screenshots ac1..ac4
            Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                val imgs = listOf(
                    com.ujwal.halter.R.drawable.ac1,
                    com.ujwal.halter.R.drawable.ac2,
                    com.ujwal.halter.R.drawable.ac3,
                    com.ujwal.halter.R.drawable.ac4
                )
                Image(painter = painterResource(id = imgs[imageIndex % imgs.size]), contentDescription = null, modifier = Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Fit)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = onPrevImage, enabled = imageIndex > 0) { Text("Previous") }
                Button(onClick = onNextImage) { Text("Next image") }
            }
            Text("Follow these screenshots when enabling Accessibility for APK installs.", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("If you installed from an app store, follow the system Accessibility prompt when requested.", style = MaterialTheme.typography.bodyMedium)
        }

        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth(), enabled = !granted) { Text(if (granted) "Already enabled" else "Open Accessibility settings") }
    }
}

@Composable
private fun PermissionInfoSimple(title: String, body: String, onOpen: () -> Unit, granted: Boolean) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth(), enabled = !granted) { Text(if (granted) "Already enabled" else "Open settings") }
    }
}

@Composable
private fun FinalPage() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("All set", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("You can change any permission later from Settings. Tap Finish to continue.", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DisposableOnResume(owner: androidx.lifecycle.LifecycleOwner, onResume: () -> Unit) {
    val lifecycle = owner.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) onResume() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}
