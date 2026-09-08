package by.iposdev.watchso.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

private val ExpressiveWearColorScheme = ColorScheme(
    primary = WearPrimary,
    onPrimary = WearOnPrimary,
    primaryContainer = WearPrimaryContainer,
    onPrimaryContainer = WearOnPrimaryContainer,
    secondary = WearSecondary,
    onSecondary = WearOnSecondary,
    secondaryContainer = WearSecondaryContainer,
    onSecondaryContainer = WearOnSecondaryContainer,
    tertiary = WearTertiary,
    onTertiary = WearOnTertiary,
    tertiaryContainer = WearTertiaryContainer,
    onTertiaryContainer = WearOnTertiaryContainer,
    surfaceContainer = WearSurfaceContainer,
    surfaceContainerLow = WearSurface,
    surfaceContainerHigh = WearSurfaceContainerHigh,
    onSurface = WearOnSurface,
    onSurfaceVariant = WearOnSurfaceVariant,
    background = WearBackground,
    onBackground = WearOnBackground,
    error = WearError,
    onError = WearOnError,
    errorContainer = WearErrorContainer,
    onErrorContainer = WearOnErrorContainer
)

@Composable
fun MitsoTestTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ExpressiveWearColorScheme,
        content = content
    )
}
