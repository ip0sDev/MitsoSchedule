package by.iposdev.watchso.presentation.components

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import mitsoschedule.core.ui.ContextUiStrings
import androidx.compose.ui.res.stringResource
import by.iposdev.watchso.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import mitsoschedule.core.schedule.TodayScheduleState
import mitsoschedule.core.schedule.TodayTimeInfo

@Composable
fun WearTodayStatusBanner(
    todayInfo: TodayTimeInfo,
    modifier: Modifier = Modifier
) {
    if (todayInfo.state == TodayScheduleState.NO_LESSONS) return

    val context = LocalContext.current
    val (infoMessage, detailMessage) = remember(todayInfo) { todayInfo.messages(ContextUiStrings(context)) }

    val (bg, border, textPrimary, textSecondary) = when (todayInfo.state) {
        TodayScheduleState.ONGOING_LESSON -> Quadruple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        TodayScheduleState.BREAK_BETWEEN_LESSONS -> Quadruple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        TodayScheduleState.FINISHED -> Quadruple(
            MaterialTheme.colorScheme.surfaceContainer,
            Color.Transparent,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        else -> Quadruple(
            MaterialTheme.colorScheme.surfaceContainer,
            Color.Transparent,
            MaterialTheme.colorScheme.onSurface,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .then(if (border != Color.Transparent) Modifier.border(1.dp, border, RoundedCornerShape(14.dp)) else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (todayInfo.state == TodayScheduleState.ONGOING_LESSON) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
            )
            Spacer(modifier = Modifier.width(7.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = infoMessage,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (detailMessage.isNotBlank()) {
                    Text(
                        text = detailMessage,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun WearDayHeader(
    dayTitle: String,
    isToday: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        val shape = RoundedCornerShape(100.dp)
        Box(
            modifier = Modifier
                .clip(shape)
                .background(if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                .then(if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
                .padding(horizontal = 12.dp, vertical = 3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isToday) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (isToday) stringResource(R.string.today_with_title, dayTitle) else dayTitle,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
