package mitsoschedule.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

/*
 * Biolume §6 — движение.
 *
 * Два сценария, и только два: «биопульс» для живых состояний (§6.1) и морфинг
 * рельефа при нажатии (§6.2). Всё остальное движение в приложении — штатные переходы M3.
 */

/** §6.1 Полный цикл биопульса. Reverse-повтор, поэтому длительность tween'а — половина. */
private const val BiopulsePeriodMs = 2400

/** CSS ease-in-out: симметричный вдох-выдох, как требует §6.1. */
private val BiopulseEasing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)

/**
 * §8 Уважение к системной настройке анимаций.
 *
 * Читается один раз на композицию: смена масштаба анимаций в системе пересоздаёт
 * Activity, так что подписываться на изменение не нужно.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

/**
 * §6.1 «Биопульс» — медленное дыхание сигнального слоя: 2400 мс, ease-in-out, 0 → 1 → 0.
 *
 * При выключенных системных анимациях возвращает константу пиковой фазы: живое состояние
 * обязано остаться видимым, просто перестаёт двигаться (§6.1, §8).
 */
@Composable
fun rememberBiopulse(): State<Float> {
    if (rememberReduceMotion()) {
        return remember { mutableFloatStateOf(1f) }
    }
    val transition = rememberInfiniteTransition(label = "biopulse")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = BiopulsePeriodMs / 2, easing = BiopulseEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "biopulse",
    )
}

/**
 * §6.1 Готовое живое свечение: радиус и плотность дышат синхронно.
 *
 * Помнить про §1 правило 2 — одновременно на экране светится максимум один элемент.
 */
@Composable
fun Modifier.biolumeBiopulse(
    shape: Shape,
    color: Color,
    minRadius: Dp = 10.dp,
    maxRadius: Dp = 20.dp,
    minAlpha: Float = 0.22f,
    maxAlpha: Float = 0.45f,
): Modifier {
    val pulse by rememberBiopulse()
    return this.biolumeGlow(
        shape = shape,
        color = color.copy(alpha = minAlpha + (maxAlpha - minAlpha) * pulse),
        radius = lerp(minRadius, maxRadius, pulse),
    )
}

/**
 * §6.2 Нажатие: рельеф перетекает Raised → Inset.
 *
 * Масштабирования нет намеренно — гайд запрещает: уезжающая под пальцем геометрия
 * читается как дешёвая анимация, тогда как смена светотени ощущается физически.
 *
 * [glowColor] — сигнальный слой на время нажатия (§4.2). Для нейтральных элементов
 * оставить null: свечение положено только акцентным CTA.
 */
@Composable
fun Modifier.biolumePressable(
    interactionSource: InteractionSource,
    shape: Shape,
    tokens: BiolumeDepthTokens,
    glowColor: Color? = null,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val progress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 400f,
            visibilityThreshold = Spring.DefaultDisplacementThreshold,
        ),
        label = "pressDepth",
    )

    return this
        .then(
            if (glowColor != null && progress > 0.01f) {
                Modifier.biolumeGlow(
                    shape = shape,
                    color = glowColor.copy(alpha = glowColor.alpha * progress),
                    radius = tokens.glowRadius,
                )
            } else {
                Modifier
            }
        )
        .biolumeRaised(shape, tokens, intensity = 1f - progress)
        .biolumeInset(shape, tokens, intensity = progress)
}
