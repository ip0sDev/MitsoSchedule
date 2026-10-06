package mitsoschedule.app.ui.components

import androidx.compose.ui.graphics.Shape
import mitsoschedule.app.ui.theme.BiolumeShapes
import androidx.compose.ui.res.stringResource
import mitsoschedule.app.R
import androidx.compose.foundation.BorderStroke
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
import mitsoschedule.app.ui.theme.BiolumeTheme
import mitsoschedule.app.ui.theme.biolumeBiopulse
import mitsoschedule.app.ui.theme.biolumeSurface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.PlayArrow
import mitsoschedule.core.model.Lesson
import mitsoschedule.core.model.SubgroupInfo
import mitsoschedule.app.ui.theme.ExamBadgeBgDark
import mitsoschedule.app.ui.theme.ExamBadgeBgLight
import mitsoschedule.app.ui.theme.ExamBadgeTextDark
import mitsoschedule.app.ui.theme.ExamBadgeTextLight
import mitsoschedule.app.ui.theme.LabBadgeBgDark
import mitsoschedule.app.ui.theme.LabBadgeBgLight
import mitsoschedule.app.ui.theme.LabBadgeTextDark
import mitsoschedule.app.ui.theme.LabBadgeTextLight
import mitsoschedule.app.ui.theme.LectureBadgeBgDark
import mitsoschedule.app.ui.theme.LectureBadgeBgLight
import mitsoschedule.app.ui.theme.LectureBadgeTextDark
import mitsoschedule.app.ui.theme.LectureBadgeTextLight
import mitsoschedule.app.ui.theme.PracticeBadgeBgDark
import mitsoschedule.app.ui.theme.PracticeBadgeBgLight
import mitsoschedule.app.ui.theme.PracticeBadgeTextDark
import mitsoschedule.app.ui.theme.PracticeBadgeTextLight

@Composable
fun LessonCard(
    lesson: Lesson,
    isCurrent: Boolean = false,
    isUpcomingFirst: Boolean = false,
    shape: Shape = RoundedCornerShape(BiolumeShapes.CardLarge),
    modifier: Modifier = Modifier
) {
    val depth = BiolumeTheme.depth
    val isDark = depth.isDark

    // §2: грань карточки одна. В покое это нейтральный hairline outlineVariant,
    // у текущей/ближайшей пары он превращается в сигнальный контур primary (§4.2).
    // Раньше рамок было две — своя border() поверх border() внутри модификатора рельефа.
    val outlineColor = when {
        isCurrent -> MaterialTheme.colorScheme.primary
        isUpcomingFirst -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val outlineWidth = if (isCurrent) 1.5.dp else 1.dp

    // surfaceContainerHigh отличим от surfaceContainer в обеих палитрах Biolume,
    // поэтому текущая пара подсвечивается фоном и в светлой теме тоже.
    val cardBg = if (isCurrent) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .biolumeSurface(
                shape = shape,
                tokens = depth,
                containerColor = cardBg,
                outlineColor = outlineColor,
                borderWidth = outlineWidth,
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Live / Status Indicator for Current or Upcoming First
            if (isCurrent || isUpcomingFirst) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCurrent) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onPrimary)
                                        .biolumeBiopulse(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            minRadius = 4.dp,
                                            maxRadius = 8.dp
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.now_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    } else if (isUpcomingFirst) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.first_lesson_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 1. ВРЕМЯ И ТИП ЗАНЯТИЯ (Отдельный выразительный блок)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!lesson.time.isNullOrBlank()) {
                    Surface(
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = stringResource(R.string.lesson_time),
                                tint = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = lesson.time.orEmpty(),
                                style = BiolumeTheme.dataType.dataMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                if (!lesson.type.isNullOrBlank()) {
                    val (badgeBg, badgeText) = getLessonTypeColors(lesson.type.orEmpty(), isDark)
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = lesson.type.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. НАЗВАНИЕ ПРЕДМЕТА
            Text(
                text = lesson.subject,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.titleMedium.lineHeight
            )

            // 3. ПОДГРУППЫ (если есть объединённые занятия)
            if (lesson.subgroups.isNotEmpty()) {
                BiolumeDivider(modifier = Modifier.padding(vertical = 12.dp))
                lesson.subgroups.forEach { subgroup ->
                    SubgroupItemView(subgroup = subgroup, parentType = lesson.type)
                }
            } else if (!lesson.room.isNullOrBlank() || !lesson.teacher.isNullOrBlank()) {
                // ОБЫЧНЫЙ ВАРИАНТ (БЕЗ ПОДГРУПП)
                BiolumeDivider(modifier = Modifier.padding(vertical = 14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // КАРТОЧКА АУДИТОРИИ
                    if (!lesson.room.isNullOrBlank()) {
                        DetailBlockCard(
                            icon = Icons.Outlined.MeetingRoom,
                            label = stringResource(R.string.room),
                            value = lesson.room.orEmpty(),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            iconColor = MaterialTheme.colorScheme.primary,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // КАРТОЧКА ПРЕПОДАВАТЕЛЯ
                    if (!lesson.teacher.isNullOrBlank()) {
                        DetailBlockCard(
                            icon = Icons.Outlined.Person,
                            label = stringResource(R.string.teacher),
                            value = lesson.teacher.orEmpty(),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            iconColor = MaterialTheme.colorScheme.primary,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(if (!lesson.room.isNullOrBlank()) 1.2f else 1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubgroupItemView(
    subgroup: SubgroupInfo,
    parentType: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Header Row: Subgroup Badge + Room Badge (ВОЗЛЕ подгруппы) + Type (if different)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Subgroup Pill
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = subgroup.subgroup,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Room Pill right BESIDE the subgroup!
                    if (!subgroup.room.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MeetingRoom,
                                    contentDescription = stringResource(R.string.room),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = subgroup.room.orEmpty(),
                                    style = BiolumeTheme.dataType.dataSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (!subgroup.type.isNullOrBlank() && subgroup.type != parentType) {
                    Text(
                        text = subgroup.type.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Teacher underneath
            if (!subgroup.teacher.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = stringResource(R.string.teacher),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = subgroup.teacher.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailBlockCard(
    icon: ImageVector,
    label: String,
    value: String,
    containerColor: Color,
    iconColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = iconColor.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 2
                )
            }
        }
    }
}

private fun getLessonTypeColors(type: String, isDark: Boolean): Pair<Color, Color> {
    val lower = type.lowercase()
    return when {
        lower.contains("лек") -> if (isDark) Pair(LectureBadgeBgDark, LectureBadgeTextDark) else Pair(LectureBadgeBgLight, LectureBadgeTextLight)
        lower.contains("практ") || lower.contains("сем") -> if (isDark) Pair(PracticeBadgeBgDark, PracticeBadgeTextDark) else Pair(PracticeBadgeBgLight, PracticeBadgeTextLight)
        lower.contains("лаб") -> if (isDark) Pair(LabBadgeBgDark, LabBadgeTextDark) else Pair(LabBadgeBgLight, LabBadgeTextLight)
        lower.contains("зачет") || lower.contains("экзамен") -> if (isDark) Pair(ExamBadgeBgDark, ExamBadgeTextDark) else Pair(ExamBadgeBgLight, ExamBadgeTextLight)
        else -> if (isDark) Pair(PracticeBadgeBgDark, PracticeBadgeTextDark) else Pair(PracticeBadgeBgLight, PracticeBadgeTextLight)
    }
}
