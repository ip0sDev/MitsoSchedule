package mitsoschedule.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import mitsoschedule.app.ui.theme.biolumeNeumorphicRaised
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Class
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mitsoschedule.app.data.DaySchedule
import mitsoschedule.app.data.Lesson
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
import java.time.LocalDate

enum class DayTimelineCategory {
    PAST, TODAY, FUTURE
}

fun classifyDaySchedule(daySchedule: DaySchedule, today: LocalDate = LocalDate.now()): DayTimelineCategory {
    val subtitle = daySchedule.dateSubtitle?.trim().orEmpty()
    val fullTitle = daySchedule.dayTitle.trim()

    val dateText = if (subtitle.isNotBlank()) subtitle else {
        fullTitle.substringAfter(",").trim()
    }

    val parts = dateText.split("\\s+".toRegex())
    if (parts.size >= 2) {
        val dayNum = parts[0].toIntOrNull()
        val monthName = parts[1].lowercase()
        val monthNum = when {
            monthName.startsWith("янв") -> 1
            monthName.startsWith("фев") -> 2
            monthName.startsWith("мар") -> 3
            monthName.startsWith("апр") -> 4
            monthName.startsWith("май") || monthName.startsWith("мая") -> 5
            monthName.startsWith("июн") -> 6
            monthName.startsWith("июл") -> 7
            monthName.startsWith("авг") -> 8
            monthName.startsWith("сен") -> 9
            monthName.startsWith("окт") -> 10
            monthName.startsWith("ноя") -> 11
            monthName.startsWith("дек") -> 12
            else -> 0
        }

        if (dayNum != null && monthNum > 0) {
            val currentYear = today.year
            val inferredYear = if (today.monthValue == 1 && monthNum == 12) {
                currentYear - 1
            } else if (today.monthValue == 12 && monthNum == 1) {
                currentYear + 1
            } else {
                currentYear
            }

            try {
                val parsedDate = LocalDate.of(inferredYear, monthNum, dayNum)
                return when {
                    parsedDate.isBefore(today) -> DayTimelineCategory.PAST
                    parsedDate.isEqual(today) -> DayTimelineCategory.TODAY
                    else -> DayTimelineCategory.FUTURE
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    return DayTimelineCategory.FUTURE
}

@Composable
fun PastDaysAccordionCard(
    pastDaysCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .biolumeNeumorphicRaised(shape = RoundedCornerShape(20.dp), isDark = isDark)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(20.dp))
            .clickable { onToggleExpand() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "Прошедшие дни недели ($pastDaysCount)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun DayScheduleSection(
    daySchedule: DaySchedule,
    isToday: Boolean = false,
    modifier: Modifier = Modifier
) {
    val activeLessons = daySchedule.lessons.filter { !it.isEmptyWindow }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        // Expressive Day Header
        DayHeader(
            dayTitle = daySchedule.dayTitle,
            activeLessonsCount = activeLessons.size,
            weekName = daySchedule.weekName,
            isToday = isToday
        )

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
                        text = "В этот день пар нет",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            activeLessons.forEach { lesson ->
                LessonCard(
                    lesson = lesson,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
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
                            text = "СЕГОДНЯ",
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
                text = if (activeLessonsCount > 0) "$activeLessonsCount ${getPairWord(activeLessonsCount)}" else "Свободно",
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

private fun getPairWord(count: Int): String {
    val lastDigit = count % 10
    val lastTwoDigits = count % 100
    return when {
        lastTwoDigits in 11..19 -> "пар"
        lastDigit == 1 -> "пара"
        lastDigit in 2..4 -> "пары"
        else -> "пар"
    }
}

@Composable
fun LessonCard(
    lesson: Lesson,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .biolumeNeumorphicRaised(shape = RoundedCornerShape(24.dp), isDark = isDark)
            .background(MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. ВРЕМЯ И ТИП ЗАНЯТИЯ (Отдельный выразительный блок)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!lesson.time.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = "Время пары",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = lesson.time,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                if (!lesson.type.isNullOrBlank()) {
                    val (badgeBg, badgeText) = getLessonTypeColors(lesson.type, isDark)
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = lesson.type,
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

            // 3. ОТДЕЛЬНЫЕ КАРТОЧКИ: АУДИТОРИЯ И ПРЕПОДАВАТЕЛЬ
            if (!lesson.room.isNullOrBlank() || !lesson.teacher.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // КАРТОЧКА АУДИТОРИИ
                    if (!lesson.room.isNullOrBlank()) {
                        DetailBlockCard(
                            icon = Icons.Outlined.MeetingRoom,
                            label = "Аудитория",
                            value = lesson.room,
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
                            label = "Преподаватель",
                            value = lesson.teacher,
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

@Composable
fun EmptyScheduleState(
    message: String,
    onSelectGroupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Расписание не выбрано",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message.ifBlank { "Выберите ваш факультет, курс и группу, чтобы просмотреть актуальное расписание занятий." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSelectGroupClick,
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.School, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Выбрать группу")
            }
        }
    }
}

@Composable
fun LoadingScheduleState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Загрузка расписания...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorScheduleState(
    errorMessage: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(44.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Не удалось загрузить",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            FilledTonalButton(
                onClick = onRetryClick,
                shape = RoundedCornerShape(100.dp)
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Повторить")
            }
        }
    }
}
