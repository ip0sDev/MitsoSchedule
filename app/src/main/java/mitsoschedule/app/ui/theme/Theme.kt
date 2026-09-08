package mitsoschedule.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Biolume Abyss ColorScheme (Dark)
private val AbyssColorScheme = darkColorScheme(
    primary = AbyssPrimary,
    onPrimary = AbyssOnPrimary,
    primaryContainer = AbyssPrimaryContainer,
    onPrimaryContainer = AbyssOnPrimaryContainer,
    secondary = AbyssSecondary,
    onSecondary = AbyssOnSecondary,
    secondaryContainer = AbyssSecondaryContainer,
    onSecondaryContainer = AbyssOnSecondaryContainer,
    tertiary = AbyssTertiary,
    onTertiary = AbyssOnTertiary,
    tertiaryContainer = AbyssTertiaryContainer,
    onTertiaryContainer = AbyssOnTertiaryContainer,
    background = AbyssBackground,
    onBackground = AbyssOnSurface,
    surface = AbyssSurface,
    onSurface = AbyssOnSurface,
    surfaceVariant = AbyssSurfaceContainerLow,
    onSurfaceVariant = AbyssOnSurfaceVariant,
    outline = AbyssOutline,
    outlineVariant = AbyssOutlineVariant,
    surfaceContainerLowest = AbyssBackground,
    surfaceContainerLow = AbyssSurfaceContainerLow,
    surfaceContainer = AbyssSurfaceContainer,
    surfaceContainerHigh = AbyssSurfaceContainerHigh,
    surfaceContainerHighest = AbyssSurfaceContainerHighest,
    error = BiolumeErrorDark
)

// Biolume Tidepool ColorScheme (Light)
private val TidepoolColorScheme = lightColorScheme(
    primary = TidepoolPrimary,
    onPrimary = TidepoolOnPrimary,
    primaryContainer = TidepoolPrimaryContainer,
    onPrimaryContainer = TidepoolOnPrimaryContainer,
    secondary = TidepoolSecondary,
    onSecondary = TidepoolOnSecondary,
    secondaryContainer = TidepoolSecondaryContainer,
    onSecondaryContainer = TidepoolOnSecondaryContainer,
    tertiary = TidepoolTertiary,
    onTertiary = TidepoolOnTertiary,
    tertiaryContainer = TidepoolTertiaryContainer,
    onTertiaryContainer = TidepoolOnTertiaryContainer,
    background = TidepoolBackground,
    onBackground = TidepoolOnSurface,
    surface = TidepoolSurface,
    onSurface = TidepoolOnSurface,
    surfaceVariant = TidepoolSurfaceContainerLow,
    onSurfaceVariant = TidepoolOnSurfaceVariant,
    outline = TidepoolOutline,
    outlineVariant = TidepoolOutlineVariant,
    surfaceContainerLowest = TidepoolBackground,
    surfaceContainerLow = TidepoolSurfaceContainerLow,
    surfaceContainer = TidepoolSurfaceContainer,
    surfaceContainerHigh = TidepoolSurfaceContainerHigh,
    surfaceContainerHighest = TidepoolSurfaceContainerHighest,
    error = BiolumeErrorLight
)

@Composable
fun MitsoTestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Biolume custom palette takes precedence over dynamic color
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AbyssColorScheme else TidepoolColorScheme
    val selectionFill = if (darkTheme) AbyssSelectionFill else TidepoolSelectionFill

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.surfaceContainer.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalBiolumeSelectionFill provides selectionFill
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
