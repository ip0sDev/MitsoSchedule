package mitsoschedule.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Токены двух слоёв глубины Biolume (§4 гайда).
 *
 * Раздаются [CompositionLocal]'ом рядом с MaterialTheme (§9.2 п.1). Модификаторы глубины —
 * чистые не-composable функции над этими токенами (§9.2 п.6), поэтому их можно ставить
 * в условные ветки модификаторных цепочек.
 *
 * [isDark] живёт здесь намеренно: компоненты не имеют права звать isSystemInDarkTheme()
 * напрямую, иначе пользовательский выбор темы рассинхронизируется с рельефом.
 */
@Immutable
data class BiolumeDepthTokens(
    val isDark: Boolean,

    // §4.1 Raised: "6px 6px 14px shadow, -5px -5px 12px highlight"
    val raisedShadowColor: Color,
    val raisedShadowOffset: Dp,
    val raisedShadowBlur: Dp,
    val raisedHighlightColor: Color,
    val raisedHighlightOffset: Dp,
    val raisedHighlightBlur: Dp,

    // §4.1 Inset: "inset 4px 4px 8px shadow, inset -3px -3px 6px highlight"
    val insetShadowColor: Color,
    val insetShadowOffset: Dp,
    val insetShadowBlur: Dp,
    val insetHighlightColor: Color,
    val insetHighlightOffset: Dp,
    val insetHighlightBlur: Dp,

    // §4.2 Сигнальный слой — только focus / press / live
    val glowPrimary: Color,
    val glowSecondary: Color,
    val glowTertiary: Color,
    val glowRadius: Dp,

    // §4.2 Плотная непрозрачная заливка выбора. НЕ primaryContainer (§10).
    val selectionFill: Color,
)

/** §4.1 Abyss: тени резче, т.к. окружающего света нет. */
val AbyssDepthTokens = BiolumeDepthTokens(
    isDark = true,
    raisedShadowColor = Color(0x73000000), // rgba(0,0,0,.45)
    raisedShadowOffset = 6.dp,
    raisedShadowBlur = 14.dp,
    raisedHighlightColor = Color(0x06FFFFFF), // rgba(255,255,255,.025)
    raisedHighlightOffset = 5.dp,
    raisedHighlightBlur = 12.dp,
    insetShadowColor = Color(0x73000000),
    insetShadowOffset = 4.dp,
    insetShadowBlur = 8.dp,
    insetHighlightColor = Color(0x08FFFFFF), // rgba(255,255,255,.03)
    insetHighlightOffset = 3.dp,
    insetHighlightBlur = 6.dp,
    glowPrimary = AbyssGlowPrimary,
    glowSecondary = AbyssGlowSecondary,
    glowTertiary = AbyssGlowTertiary,
    glowRadius = 12.dp,
    selectionFill = AbyssSelectionFill,
)

/** §4.1 Tidepool: тот же рельеф, но мягче — дневной свет уже есть. */
val TidepoolDepthTokens = BiolumeDepthTokens(
    isDark = false,
    raisedShadowColor = Color(0x14000000), // rgba(0,0,0,.08)
    raisedShadowOffset = 6.dp,
    raisedShadowBlur = 14.dp,
    raisedHighlightColor = Color(0xE6FFFFFF), // rgba(255,255,255,.9)
    raisedHighlightOffset = 5.dp,
    raisedHighlightBlur = 12.dp,
    insetShadowColor = Color(0x14000000),
    insetShadowOffset = 4.dp,
    insetShadowBlur = 8.dp,
    insetHighlightColor = Color(0xE6FFFFFF),
    insetHighlightOffset = 3.dp,
    insetHighlightBlur = 6.dp,
    glowPrimary = TidepoolGlowPrimary,
    glowSecondary = TidepoolGlowSecondary,
    glowTertiary = TidepoolGlowTertiary,
    glowRadius = 10.dp,
    selectionFill = TidepoolSelectionFill,
)

/**
 * success / warning — у Material 3 таких ролей нет, а Biolume §3 их требует.
 * Раздаются отдельно, чтобы компоненты не хардкодили Color(0xFF...).
 */
@Immutable
data class BiolumeStatusColors(
    val success: Color,
    val warning: Color,
)

val AbyssStatusColors = BiolumeStatusColors(
    success = BiolumeSuccessDark,
    warning = BiolumeWarningDark,
)

val TidepoolStatusColors = BiolumeStatusColors(
    success = BiolumeSuccessLight,
    warning = BiolumeWarningLight,
)

val LocalBiolumeDepth = staticCompositionLocalOf { AbyssDepthTokens }
val LocalBiolumeStatus = staticCompositionLocalOf { AbyssStatusColors }

/** Единый аксессор к надстройке Biolume поверх MaterialTheme (§9.2 п.1). */
object BiolumeTheme {
    val depth: BiolumeDepthTokens
        @Composable @ReadOnlyComposable get() = LocalBiolumeDepth.current

    val status: BiolumeStatusColors
        @Composable @ReadOnlyComposable get() = LocalBiolumeStatus.current

    val dataType: BiolumeDataTypography
        @Composable @ReadOnlyComposable get() = LocalBiolumeDataType.current
}
