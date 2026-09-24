package mitsoschedule.app.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Biolume §4 — два независимых слоя глубины.
 *
 * Все модификаторы здесь — НЕ-composable чистые функции над BiolumeDepthTokens (§9.2 п.6):
 * их можно ставить в условные ветки модификаторных цепочек. Вызывающий компонент один раз
 * читает `val depth = BiolumeTheme.depth` и передаёт токены дальше.
 *
 * Рисуем через drawWithCache, а не drawBehind: Paint, BlurMaskFilter и Outline создаются
 * один раз на размер, а не на каждом кадре. Модификатор висит на каждой карточке LazyColumn,
 * поэтому аллокации в draw-фазе здесь стоят дорого.
 *
 * Ни один модификатор не рисует рамку сам — грань outlineVariant добавляет компонент
 * (§2, гибрид Elevated + Outlined). Иначе получается двойной контур.
 */

/**
 * §4.1 Raised — состояние по умолчанию: карточки, tonal/outlined-кнопки, чипы, thumb тумблера.
 * Мягкий двойной свет от одного невидимого источника сверху-слева.
 *
 * Тень рисуется ВНЕ границ компонента, поэтому вокруг нужно ≥6–8 dp воздуха (§7),
 * иначе плотный родитель её срежет.
 *
 * [intensity] нужен только для морфинга Raised→Inset при нажатии (§6.2); в покое — 1.
 */
fun Modifier.biolumeRaised(
    shape: Shape,
    tokens: BiolumeDepthTokens,
    intensity: Float = 1f,
): Modifier = if (intensity <= 0.01f) this else this.drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)

    // §9.2: setShadowLayer, т.к. штатный Modifier.shadow не умеет ни смещение, ни контр-тень.
    // Цвет самой кисти прозрачный — на холст попадает только слой тени.
    val highlightPaint = Paint().apply {
        color = Color.Transparent
        nativePaint.setShadowLayer(
            tokens.raisedHighlightBlur.toPx(),
            -tokens.raisedHighlightOffset.toPx(),
            -tokens.raisedHighlightOffset.toPx(),
            tokens.raisedHighlightColor.scaleAlpha(intensity).toArgb(),
        )
    }
    val shadowPaint = Paint().apply {
        color = Color.Transparent
        nativePaint.setShadowLayer(
            tokens.raisedShadowBlur.toPx(),
            tokens.raisedShadowOffset.toPx(),
            tokens.raisedShadowOffset.toPx(),
            tokens.raisedShadowColor.scaleAlpha(intensity).toArgb(),
        )
    }

    onDrawBehind {
        drawIntoCanvas { canvas ->
            canvas.drawOutline(outline, highlightPaint)
            canvas.drawOutline(outline, shadowPaint)
        }
    }
}

/**
 * §4.1 Inset — «врезано»: текстовые поля, трек тумблера, нажатая кнопка, выбранный чип.
 *
 * Inset-теней в Compose из коробки нет (§9.2). Рисуем контур фигуры размытой обводкой
 * со смещением, обрезая всё за пределами фигуры: внутрь попадает только половина обводки.
 *
 * Рисуется ПОВЕРХ содержимого, а не за ним: вдавленность — это тень на самой заливке.
 * Тень за заливкой была бы ею же и закрыта. Обводка мягкая и прижата к краям, поэтому
 * текст с нормальными отступами она не задевает.
 *
 * [intensity] нужен только для морфинга Raised→Inset при нажатии (§6.2).
 */
fun Modifier.biolumeInset(
    shape: Shape,
    tokens: BiolumeDepthTokens,
    intensity: Float = 1f,
): Modifier = if (intensity <= 0.01f) this else this.drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }

    fun insetPaint(colorValue: Color, blur: Dp) = Paint().apply {
        color = colorValue.scaleAlpha(intensity)
        style = PaintingStyle.Stroke
        // Обводка шириной 2×blur ложится по контуру симметрично; видимой остаётся внутренняя половина.
        strokeWidth = blur.toPx() * 2f
        nativePaint.maskFilter = BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL)
    }

    val shadowPaint = insetPaint(tokens.insetShadowColor, tokens.insetShadowBlur)
    val highlightPaint = insetPaint(tokens.insetHighlightColor, tokens.insetHighlightBlur)
    val shadowOffset = tokens.insetShadowOffset.toPx()
    val highlightOffset = tokens.insetHighlightOffset.toPx()

    onDrawWithContent {
        drawContent()
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.clipPath(path)

            // Тёмная — от верхне-левого внутреннего края.
            canvas.save()
            canvas.translate(shadowOffset, shadowOffset)
            canvas.drawPath(path, shadowPaint)
            canvas.restore()

            // Светлая контр-подсветка — от нижне-правого.
            canvas.save()
            canvas.translate(-highlightOffset, -highlightOffset)
            canvas.drawPath(path, highlightPaint)
            canvas.restore()

            canvas.restore()
        }
    }
}

/**
 * §4.2 Сигнальный слой — несмещённое свечение поверх структуры.
 *
 * Только focus / press / live. Никогда не в состоянии покоя, и максимум один на экране (§1 п.2).
 */
fun Modifier.biolumeGlow(
    shape: Shape,
    color: Color,
    radius: Dp,
): Modifier = this.drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val glowPaint = Paint().apply {
        this.color = Color.Transparent
        nativePaint.setShadowLayer(radius.toPx(), 0f, 0f, color.toArgb())
    }
    onDrawBehind {
        drawIntoCanvas { canvas -> canvas.drawOutline(outline, glowPaint) }
    }
}

/**
 * §2 Карточка Biolume: неоморфный рельеф + hairline-грань outlineVariant
 * (гибрид Elevated Card и Outlined Card из M3).
 *
 * [outlineColor] — обычно `outlineVariant`. Для живого/выбранного элемента сюда приходит
 * сигнальный контур `primary` (§4.2), и тогда рамка ровно одна, а не две вложенные.
 */
fun Modifier.biolumeSurface(
    shape: Shape,
    tokens: BiolumeDepthTokens,
    containerColor: Color,
    outlineColor: Color,
    borderWidth: Dp = 1.dp,
): Modifier = this
    .biolumeRaised(shape, tokens)
    .clip(shape)
    .background(containerColor)
    .border(borderWidth, outlineColor, shape)

/**
 * §7 «Зазор под рельеф»: в плотных списочных рядах мягкие тени соседей накладываются
 * и читаются грязными полосами. Там, где воздуха нет, — только нейтральная грань, без тени.
 */
fun Modifier.biolumeHairline(
    shape: Shape,
    containerColor: Color,
    outlineColor: Color,
    borderWidth: Dp = 1.dp,
): Modifier = this
    .clip(shape)
    .background(containerColor)
    .border(borderWidth, outlineColor, shape)

/** Плавное гашение слоя глубины при морфинге Raised↔Inset (§6.2). */
private fun Color.scaleAlpha(factor: Float): Color =
    if (factor >= 1f) this else copy(alpha = alpha * factor.coerceIn(0f, 1f))
