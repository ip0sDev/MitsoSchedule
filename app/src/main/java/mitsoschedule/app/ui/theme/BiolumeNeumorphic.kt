package mitsoschedule.app.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Biolume §4.1 Soft Neumorphic Raised Modifier.
 * Draws soft dual shadows (Top-Left light highlight + Bottom-Right dark shadow)
 * and dual gradient highlight border.
 */
fun Modifier.biolumeNeumorphicRaised(
    shape: Shape = RoundedCornerShape(24.dp),
    isDark: Boolean = true
): Modifier = this
    .drawBehind {
        drawIntoCanvas { canvas ->
            val darkShadowColor = if (isDark) Color(0x99000000) else Color(0x26000000)
            val lightHighlightColor = if (isDark) Color(0x12FFFFFF) else Color(0xE6FFFFFF)

            val pxOffsetX = 6.dp.toPx()
            val pxOffsetY = 6.dp.toPx()
            val pxBlurDark = 14.dp.toPx()
            val pxBlurLight = 12.dp.toPx()

            // 1. Top-Left Light Counter-Highlight Shadow
            val lightPaint = Paint().apply {
                nativePaint.color = lightHighlightColor.toArgb()
                nativePaint.maskFilter = BlurMaskFilter(pxBlurLight, BlurMaskFilter.Blur.NORMAL)
            }

            // 2. Bottom-Right Dark Drop Shadow
            val darkPaint = Paint().apply {
                nativePaint.color = darkShadowColor.toArgb()
                nativePaint.maskFilter = BlurMaskFilter(pxBlurDark, BlurMaskFilter.Blur.NORMAL)
            }

            val outline = shape.createOutline(size, layoutDirection, this)

            // Draw light highlight offset top-left
            canvas.save()
            canvas.translate(-pxOffsetX * 0.7f, -pxOffsetY * 0.7f)
            canvas.drawOutline(outline, lightPaint)
            canvas.restore()

            // Draw dark shadow offset bottom-right
            canvas.save()
            canvas.translate(pxOffsetX, pxOffsetY)
            canvas.drawOutline(outline, darkPaint)
            canvas.restore()
        }
    }
    .border(
        width = 1.dp,
        brush = if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0x33FFFFFF), // Top-Left light edge
                    Color(0x05FFFFFF),
                    Color(0x40000000)  // Bottom-Right dark edge
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFFFF), // Top-Left light edge
                    Color(0x1F000000)  // Bottom-Right dark edge
                )
            )
        },
        shape = shape
    )
    .clip(shape)

/**
 * Biolume Neumorphic Card Container
 */
@Composable
fun BiolumeCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    isDark: Boolean = isSystemInDarkTheme(),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .biolumeNeumorphicRaised(shape = shape, isDark = isDark)
            .background(containerColor, shape = shape)
            .padding(18.dp),
        content = content
    )
}
