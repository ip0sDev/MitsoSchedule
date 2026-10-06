package mitsoschedule.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Формы карточек (§2, §7.1): шкала M3 Expressive.
 *
 * Одиночная карточка получает «очень большой» радиус [CardLarge]. Карточки, идущие подряд
 * и читающиеся как один набор (пары одного дня), собираются в «соединённый» ряд: внешние
 * края крупные, края между соседями острые ([CardSmall]). Так группа выглядит единым целым,
 * но каждая карточка остаётся отдельной.
 */
object BiolumeShapes {
    /** M3E extra-large. */
    val CardLarge: Dp = 28.dp

    /** Край карточки, примыкающий к соседу в соединённом ряду. */
    val CardSmall: Dp = 8.dp

    /** Зазор между карточками соединённого ряда. */
    val ConnectedGap: Dp = 3.dp

    /** Форма [index]-й из [count] карточек соединённого ряда. */
    fun connected(index: Int, count: Int, large: Dp = CardLarge, small: Dp = CardSmall): Shape = when {
        count <= 1 -> RoundedCornerShape(large)
        index == 0 -> RoundedCornerShape(
            topStart = large, topEnd = large, bottomStart = small, bottomEnd = small
        )
        index == count - 1 -> RoundedCornerShape(
            topStart = small, topEnd = small, bottomStart = large, bottomEnd = large
        )
        else -> RoundedCornerShape(small)
    }
}
