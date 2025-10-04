package by.iposdev.watchso.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ColorScheme

private val ExpressiveColorScheme = ColorScheme(
    primary = Color(0xFF3A4F7A), // A dark, muted blue
    onPrimary = Color.White,
    secondary = Color(0xFF006064), // A deep cyan
    onSecondary = Color.White,
    error = Color(0xFFB00020),
    onError = Color.White,
    background = Color(0xFF121212),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFBDBDBD)
)

@Composable
fun MitsoTestTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ExpressiveColorScheme,
        content = content
    )
}
