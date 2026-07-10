// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ujwal.halter.settings.ColorIntensity
import com.ujwal.halter.settings.DarkModePreference
import com.ujwal.halter.settings.HalterSettings

@Composable
fun HalterTheme(settings: HalterSettings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = when (settings.darkModePreference) {
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
        DarkModePreference.LIGHT -> false
        DarkModePreference.DARK -> true
    }
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scheme = when {
        settings.useDynamicWallpaperColor && dynamicAvailable && dark -> dynamicDarkColorScheme(context)
        settings.useDynamicWallpaperColor && dynamicAvailable -> dynamicLightColorScheme(context)
        settings.customSeedColorArgb != null -> seededScheme(settings.customSeedColorArgb, settings.colorIntensity, dark)
        dark -> expressiveDarkScheme()
        else -> expressiveLightScheme()
    }
    MaterialTheme(
        colorScheme = scheme,
        shapes = expressiveShapes(settings.cornerRadiusScale),
        content = content
    )
}

/** Expressive light scheme — higher saturation, richer surfaces. */
private fun expressiveLightScheme(): ColorScheme = lightColorScheme(
    primary = Color(0xFF6B4B9A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF0E6FF),
    onPrimaryContainer = Color(0xFF260D4D),
    secondary = Color(0xFF4A6B61),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCF1E4),
    onSecondaryContainer = Color(0xFF04201A),
    tertiary = Color(0xFF8C5A2B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCC2),
    onTertiaryContainer = Color(0xFF301800),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF3EFF4),
    onSurfaceVariant = Color(0xFF4B464B),
    outline = Color(0xFF7C767B),
    outlineVariant = Color(0xFFCDC7CD),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    surfaceTint = Color(0xFF6B4B9A)
)

/** Expressive dark scheme — deeper, more vibrant. */
private fun expressiveDarkScheme(): ColorScheme = darkColorScheme(
    primary = Color(0xFFD5BCFF),
    onPrimary = Color(0xFF3C2562),
    primaryContainer = Color(0xFF533C7A),
    onPrimaryContainer = Color(0xFFF0E6FF),
    secondary = Color(0xFFB1D5C8),
    onSecondary = Color(0xFF1B352F),
    secondaryContainer = Color(0xFF324C45),
    onSecondaryContainer = Color(0xFFCCF1E4),
    tertiary = Color(0xFFFFB77C),
    onTertiary = Color(0xFF4B2B00),
    tertiaryContainer = Color(0xFF6B4015),
    onTertiaryContainer = Color(0xFFFFDCC2),
    error = Color(0xFFFFB4AB),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E6),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E6),
    surfaceVariant = Color(0xFF4B464B),
    onSurfaceVariant = Color(0xFFCDC7CD),
    outline = Color(0xFF979197),
    outlineVariant = Color(0xFF4B464B),
    inverseSurface = Color(0xFFE6E1E6),
    inverseOnSurface = Color(0xFF313033),
    surfaceTint = Color(0xFFD5BCFF)
)

private fun seededScheme(seedArgb: Int, intensity: ColorIntensity, dark: Boolean): ColorScheme {
    val seed = Color(seedArgb)
    val primary = when (intensity) {
        ColorIntensity.NEUTRAL -> seed.copy(alpha = 0.78f)
        ColorIntensity.SOFT -> seed.copy(alpha = 0.86f)
        ColorIntensity.LIGHT -> seed.copy(alpha = 0.94f)
        ColorIntensity.EXPRESSIVE -> seed
    }
    return if (dark) {
        darkColorScheme(primary = primary, secondary = primary.copy(alpha = 0.70f), tertiary = primary.copy(alpha = 0.55f))
    } else {
        lightColorScheme(primary = primary, secondary = primary.copy(alpha = 0.75f), tertiary = primary.copy(alpha = 0.60f))
    }
}

private fun expressiveShapes(scale: Float): Shapes {
    fun radius(base: Int) = (base * scale).coerceIn(4f, 36f).dp
    return Shapes(
        extraSmall = RoundedCornerShape(radius(10)),
        small = RoundedCornerShape(radius(14)),
        medium = RoundedCornerShape(radius(20)),
        large = RoundedCornerShape(radius(26)),
        extraLarge = RoundedCornerShape(radius(32))
    )
}
