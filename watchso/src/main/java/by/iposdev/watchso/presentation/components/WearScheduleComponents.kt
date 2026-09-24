package by.iposdev.watchso.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import by.iposdev.watchso.data.Lesson
import by.iposdev.watchso.data.SubgroupInfo
import by.iposdev.watchso.data.TodayScheduleState
import by.iposdev.watchso.data.TodayTimeInfo
import by.iposdev.watchso.data.UserSelection
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
fun WearNavHeader(
    currentScreen: Int,
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WearNavPill(
            title = "Пары",
            isSelected = currentScreen == 0,
            onClick = { onNavigate(0) }
        )
        Spacer(modifier = Modifier.width(6.dp))
        WearNavPill(
            title = "Кабинет",
            isSelected = currentScreen == 1,
            onClick = { onNavigate(1) }
        )
        Spacer(modifier = Modifier.width(6.dp))
        WearNavPill(
            title = "Группа",
            isSelected = currentScreen == 2,
            onClick = { onNavigate(2) }
        )
    }
}

@Composable
private fun WearNavPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val pillModifier = if (isSelected) {
        Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
    } else {
        Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    }

    Box(
        modifier = pillModifier
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WearGroupHeaderCard(
    userSelection: UserSelection,
    onEditClick: () -> Unit,
    onRefreshClick: (() -> Unit)? = null,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onEditClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Text(
                        text = if (userSelection.groupName.isNotBlank()) userSelection.groupName else "Выбрать группу",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (userSelection.courseName.isNotBlank()) "${userSelection.courseName} • ${userSelection.formName}" else "Нажмите для выбора",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onRefreshClick != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isLoading, onClick = onRefreshClick),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Обновить расписание",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Сменить группу",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun WearWeekNavigator(
    weekName: String,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    if (canGoPrev) MaterialTheme.colorScheme.surfaceContainer
                    else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f)
                )
                .clickable(enabled = canGoPrev, onClick = onPrevClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Предыдущая неделя",
                tint = if (canGoPrev) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(14.dp)
            )
        }

        Text(
            text = weekName.ifBlank { "Текущая неделя" },
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
        )

        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    if (canGoNext) MaterialTheme.colorScheme.surfaceContainer
                    else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f)
                )
                .clickable(enabled = canGoNext, onClick = onNextClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "Следующая неделя",
                tint = if (canGoNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun WearPullToRefreshIndicator(
    pullOffset: Float,
    pullThreshold: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    if (pullOffset <= 0f && !isRefreshing) return

    val progress = (pullOffset / pullThreshold).coerceIn(0f, 1f)
    val isReady = pullOffset >= pullThreshold

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(
                if (isReady || isRefreshing) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainer
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Синхронизация...",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                tint = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer(rotationZ = progress * 360f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isReady) "Отпустите" else "Потяните вниз",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WearTodayStatusBanner(
    todayInfo: TodayTimeInfo,
    modifier: Modifier = Modifier
) {
    if (todayInfo.state == TodayScheduleState.NO_LESSONS) return

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
                    text = todayInfo.infoMessage,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (todayInfo.detailMessage.isNotBlank()) {
                    Text(
                        text = todayInfo.detailMessage,
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
                    text = if (isToday) "Сегодня • $dayTitle" else dayTitle,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

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
                    text = "Окно (нет занятий)",
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
                                text = "СЕЙЧАС",
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
                                        text = lesson.room,
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
                                text = lesson.teacher,
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
                    text = subgroup.subgroup.ifBlank { "Подгр." },
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
                        text = subgroup.room,
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
                text = subgroup.teacher,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
