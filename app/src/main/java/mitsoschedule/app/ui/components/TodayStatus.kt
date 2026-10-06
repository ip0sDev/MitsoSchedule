package mitsoschedule.app.ui.components

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import mitsoschedule.core.schedule.TodayScheduleState
import mitsoschedule.core.schedule.TodayTimeInfo
import mitsoschedule.core.ui.ContextUiStrings
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
import mitsoschedule.app.ui.theme.BiolumeTheme
import mitsoschedule.app.ui.theme.biolumeBiopulse
import mitsoschedule.app.ui.theme.biolumeSurface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FreeBreakfast
import androidx.compose.material.icons.outlined.Schedule

@Composable
fun TodayStatusBanner(
    todayInfo: TodayTimeInfo,
    modifier: Modifier = Modifier
) {
    val status = BiolumeTheme.status
    val context = LocalContext.current
    val (infoMessage, detailMessage) = remember(todayInfo) { todayInfo.messages(ContextUiStrings(context)) }

    val (containerColor, iconColor, textColor) = when (todayInfo.state) {
        TodayScheduleState.ONGOING_LESSON -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        TodayScheduleState.NOT_STARTED, TodayScheduleState.NO_LESSONS -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onSurface
        )
        TodayScheduleState.BREAK_BETWEEN_LESSONS -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onSurface
        )
        TodayScheduleState.FINISHED -> Triple(
            status.success.copy(alpha = 0.15f),
            status.success,
            status.success
        )
    }

    val icon = when (todayInfo.state) {
        TodayScheduleState.ONGOING_LESSON -> Icons.Outlined.AccessTime
        TodayScheduleState.NOT_STARTED, TodayScheduleState.NO_LESSONS -> Icons.Outlined.Schedule
        TodayScheduleState.BREAK_BETWEEN_LESSONS -> Icons.Outlined.FreeBreakfast
        TodayScheduleState.FINISHED -> Icons.Outlined.CheckCircle
    }

    val depth = BiolumeTheme.depth
    val isOngoing = todayInfo.state == TodayScheduleState.ONGOING_LESSON
    val bannerShape = RoundedCornerShape(20.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .biolumeSurface(
                shape = bannerShape,
                tokens = depth,
                containerColor = containerColor,
                outlineColor = if (isOngoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            ),
        color = containerColor,
        shape = bannerShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = iconColor.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier
                    .size(36.dp)
                    .then(
                        if (isOngoing) {
                            Modifier.biolumeBiopulse(
                                shape = CircleShape,
                                color = iconColor,
                                minRadius = 4.dp,
                                maxRadius = 10.dp
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = infoMessage,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (detailMessage.isNotBlank()) {
                    Text(
                        text = detailMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
