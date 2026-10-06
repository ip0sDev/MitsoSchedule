package mitsoschedule.app.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class BiolumeDividerStyle {
    /** Линия, плавно гаснущая к краям: разделяет содержимое внутри карточки. */
    Fade,

    /** Две гаснущие линии и «узел» посередине: разделяет крупные секции (дни расписания). */
    Node
}

/**
 * Фирменный разделитель Biolume (§7.2).
 *
 * Вместо сплошной линии на всю ширину: hairline цвета `outlineVariant`, гаснущий к краям,
 * как свет в толще воды. В стиле [BiolumeDividerStyle.Node] посередине стоит маленький узел.
 * Это структурный элемент, а не сигнал (§4.2): узел нейтральный, без свечения;
 * только [highlighted] красит его в `primary`, и то статично.
 */
@Composable
fun BiolumeDivider(
    modifier: Modifier = Modifier,
    style: BiolumeDividerStyle = BiolumeDividerStyle.Fade,
    highlighted: Boolean = false
) {
    // outlineVariant в обеих палитрах почти сливается с карточкой, поэтому берём outline с альфой
    val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val fade = Brush.horizontalGradient(listOf(Color.Transparent, lineColor, lineColor, Color.Transparent))

    when (style) {
        BiolumeDividerStyle.Fade -> Box(
            modifier = modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(fade)
        )

        BiolumeDividerStyle.Node -> Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, lineColor)))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(if (highlighted) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(
                        if (highlighted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(lineColor, Color.Transparent)))
            )
        }
    }
}
