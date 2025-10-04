package by.iposdev.watchso.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ColorScheme

private val ExpressiveColorScheme = ColorScheme(
    primary = Color(0xFFE8DEF8),       // Light purple for primary buttons
    onPrimary = Color(0xFF1D192B),     // Dark purple text on primary
    secondary = Color(0xFFCCC2DC),     // A muted purple for secondary elements
    onSecondary = Color(0xFF332D41),   // Dark text on secondary
    error = Color(0xFFB00020),
    onError = Color.White,
    background = Color.Black,          // Black background
    onBackground = Color.White,
    onSurface = Color.White,           // White text on surfaces
    onSurfaceVariant = Color(0xFFCAC4D0)// Grey for less prominent text
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
