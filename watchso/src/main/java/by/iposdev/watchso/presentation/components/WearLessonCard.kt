package by.iposdev.watchso.presentation.components

import androidx.compose.ui.res.stringResource
import by.iposdev.watchso.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import mitsoschedule.core.model.Lesson
import mitsoschedule.core.model.SubgroupInfo
import by.iposdev.watchso.presentation.theme.BiolumeBadgeActiveBg
import by.iposdev.watchso.presentation.theme.BiolumeBadgeActiveText
import by.iposdev.watchso.presentation.theme.BiolumeBadgeExamBg
import by.iposdev.watchso.presentation.theme.BiolumeBadgeExamText
import by.iposdev.watchso.presentation.theme.BiolumeBadgeLabBg
import by.iposdev.watchso.presentation.theme.BiolumeBadgeLabText
import by.iposdev.watchso.presentation.theme.BiolumeBadgeLectureBg
import by.iposdev.watchso.presentation.theme.BiolumeBadgeLectureText
import by.iposdev.watchso.presentation.theme.BiolumeBadgePracticeBg
import by.iposdev.watchso.presentation.theme.BiolumeBadgePracticeText
import by.iposdev.watchso.presentation.theme.BiolumeBadgeRoomBg
import by.iposdev.watchso.presentation.theme.BiolumeBadgeRoomBorder
import by.iposdev.watchso.presentation.theme.BiolumeBadgeRoomIcon
import by.iposdev.watchso.presentation.theme.BiolumeBadgeRoomText
import by.iposdev.watchso.presentation.theme.BiolumeBadgeTimeBg
import by.iposdev.watchso.presentation.theme.BiolumeBadgeTimeText

@Composable
fun WearLessonCard(
    lesson: Lesson,
    isCurrent: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (lesson.isEmptyWindow) {
        Card(
            onClick = {},
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                lesson.time?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = stringResource(R.string.window_no_lessons),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val cardShape = RoundedCornerShape(20.dp)
    val cardModifier = if (isCurrent) {
        modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colorScheme.primary, cardShape)
    } else {
        modifier.fillMaxWidth()
    }

    Card(
        onClick = {},
        modifier = cardModifier,
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // 1. Time, Current indicator and Type badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                lesson.time?.let { rawTime ->
                    val compactTime = rawTime.replace(" — ", "–").replace(" - ", "–").trim()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer else BiolumeBadgeTimeBg)
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = compactTime,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else BiolumeBadgeTimeText,
                                maxLines = 1
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(BiolumeBadgeActiveBg)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.now_badge),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = BiolumeBadgeActiveText,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    lesson.type?.let { typeStr ->
                        val shortType = when {
                            typeStr.contains("лаб", true) -> "Лаб"
                            typeStr.contains("практ", true) || typeStr.contains("семин", true) -> "Практ"
                            typeStr.contains("лек", true) -> "Лекция"
                            typeStr.contains("конс", true) -> "Консульт"
                            typeStr.contains("экз", true) -> "Экзамен"
                            typeStr.contains("зач", true) -> "Зачет"
                            typeStr.contains("срс", true) || typeStr.contains("самост", true) -> "СРС"
                            typeStr.length > 8 -> typeStr.take(7) + "."
                            else -> typeStr
                        }
                        val (bg, textColor) = when {
                            typeStr.contains("лек", true) -> BiolumeBadgeLectureBg to BiolumeBadgeLectureText
                            typeStr.contains("лаб", true) -> BiolumeBadgeLabBg to BiolumeBadgeLabText
                            typeStr.contains("экз", true) || typeStr.contains("зач", true) -> BiolumeBadgeExamBg to BiolumeBadgeExamText
                            else -> BiolumeBadgePracticeBg to BiolumeBadgePracticeText
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(bg)
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = shortType,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Subject Title
            Text(
                text = lesson.subject,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 14.sp
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // 3. Subgroups if present
            if (lesson.subgroups.isNotEmpty()) {
                Spacer(modifier = Modifier.height(5.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    lesson.subgroups.forEach { subgroup ->
                        WearSubgroupRow(subgroup = subgroup)
                    }
                }
            } else {
                // 4. Room & Teacher tags
                if (!lesson.room.isNullOrBlank() || !lesson.teacher.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (!lesson.room.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(BiolumeBadgeRoomBg)
                                    .border(1.dp, BiolumeBadgeRoomBorder.copy(alpha = 0.75f), RoundedCornerShape(7.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.MeetingRoom,
                                        contentDescription = null,
                                        tint = BiolumeBadgeRoomIcon,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = lesson.room.orEmpty(),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.2.sp
                                        ),
                                        color = BiolumeBadgeRoomText,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if (!lesson.teacher.isNullOrBlank()) {
                            Text(
                                text = lesson.teacher.orEmpty(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WearSubgroupRow(
    subgroup: SubgroupInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = subgroup.subgroup.ifBlank { stringResource(R.string.subgroup_short) },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }

            if (!subgroup.room.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BiolumeBadgeRoomBg)
                        .border(0.8.dp, BiolumeBadgeRoomBorder.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = subgroup.room.orEmpty(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = BiolumeBadgeRoomText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (!subgroup.teacher.isNullOrBlank()) {
            Text(
                text = subgroup.teacher.orEmpty(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
