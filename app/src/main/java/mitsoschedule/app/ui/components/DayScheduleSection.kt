package mitsoschedule.app.ui.components

import mitsoschedule.app.ui.theme.BiolumeShapes
import mitsoschedule.core.schedule.TodayScheduleState
import mitsoschedule.core.schedule.TodayStatus
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mitsoschedule.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mitsoschedule.core.model.DaySchedule

@Composable
fun DayScheduleSection(
    daySchedule: DaySchedule,
    isToday: Boolean = false,
    showDivider: Boolean = false,
    modifier: Modifier = Modifier
) {
    val activeLessons = daySchedule.lessons.filter { !it.isEmptyWindow }
    val todayInfo = if (isToday) TodayStatus.calculate(activeLessons) else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp, top = if (showDivider) 0.dp else 10.dp)
    ) {
        // Между днями: фирменный разделитель с узлом (сегодняшний день подсвечен)
        if (showDivider) {
            BiolumeDivider(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 14.dp),
                style = BiolumeDividerStyle.Node,
                highlighted = isToday
            )
        }

        // Expressive Day Header
        DayHeader(
            dayTitle = daySchedule.dayTitle,
            activeLessonsCount = activeLessons.size,
            weekName = daySchedule.weekName,
            isToday = isToday
        )

        // Today Status Banner (when lessons exist)
        if (isToday && activeLessons.isNotEmpty() && todayInfo != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TodayStatusBanner(todayInfo = todayInfo)
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (activeLessons.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Class,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isToday) stringResource(R.string.today_no_lessons) else stringResource(R.string.day_no_lessons),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Пары дня идут «соединённым рядом» (M3E): крупные внешние углы, острые между соседями
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(BiolumeShapes.ConnectedGap)
            ) {
                activeLessons.forEachIndexed { index, lesson ->
                    val isCurrent = isToday && todayInfo?.currentLessonIndex == index
                    val isUpcomingFirst = isToday && todayInfo?.state == TodayScheduleState.NOT_STARTED && index == 0

                    LessonCard(
                        lesson = lesson,
                        isCurrent = isCurrent,
                        isUpcomingFirst = isUpcomingFirst,
                        shape = BiolumeShapes.connected(index, activeLessons.size),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun DayHeader(
    dayTitle: String,
    activeLessonsCount: Int,
    weekName: String = "",
    isToday: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isToday) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.today_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (weekName.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = weekName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (isToday || weekName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = dayTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Surface(
            color = if (activeLessonsCount > 0) {
                if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
            } else MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(100.dp)
        ) {
            Text(
                text = if (activeLessonsCount > 0) pluralStringResource(R.plurals.lessons_count, activeLessonsCount, activeLessonsCount) else stringResource(R.string.free_day),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (activeLessonsCount > 0) {
                    if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                } else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
