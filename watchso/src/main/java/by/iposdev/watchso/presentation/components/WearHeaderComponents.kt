package by.iposdev.watchso.presentation.components

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import by.iposdev.watchso.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import mitsoschedule.core.model.UserSelection
import by.iposdev.watchso.presentation.WearScreen

@Composable
fun WearNavHeader(
    currentScreen: WearScreen,
    onNavigate: (WearScreen) -> Unit,
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
            title = stringResource(R.string.nav_lessons),
            isSelected = currentScreen == WearScreen.SCHEDULE,
            onClick = { onNavigate(WearScreen.SCHEDULE) }
        )
        Spacer(modifier = Modifier.width(4.dp))
        WearNavPill(
            title = stringResource(R.string.nav_cabinet),
            isSelected = currentScreen == WearScreen.CABINET,
            onClick = { onNavigate(WearScreen.CABINET) }
        )
        Spacer(modifier = Modifier.width(4.dp))
        WearNavPill(
            title = stringResource(R.string.group),
            isSelected = currentScreen == WearScreen.PICKER,
            onClick = { onNavigate(WearScreen.PICKER) }
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
            .padding(horizontal = 9.dp, vertical = 3.dp),
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

/**
 * Компактная шапка расписания: группа и неделя в одной карточке со стрелками по краям.
 * Заменяет две отдельные строки (карточку группы и навигатор недель), освобождая место под пары.
 * Нажатие на центр открывает выбор группы; обновление делается жестом «потянуть вниз».
 */
@Composable
fun WearScheduleHeader(
    userSelection: UserSelection,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onEditClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hasGroup = userSelection.isComplete
    val subtitle = when {
        !hasGroup -> stringResource(R.string.tap_to_choose_group)
        else -> userSelection.weekName.ifBlank { stringResource(R.string.current_week) }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasGroup) {
            WearHeaderArrow(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                description = stringResource(R.string.previous_week),
                enabled = canGoPrev,
                onClick = onPrevClick
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onEditClick)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (userSelection.groupName.isNotBlank()) userSelection.groupName else stringResource(R.string.choose_group),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.width(5.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.SemiBold,
                color = if (hasGroup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (hasGroup) {
            WearHeaderArrow(
                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                description = stringResource(R.string.next_week),
                enabled = canGoNext,
                onClick = onNextClick
            )
        }
    }
}

@Composable
private fun WearHeaderArrow(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(15.dp)
        )
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
                text = stringResource(R.string.syncing),
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
                text = if (isReady) stringResource(R.string.release_to_refresh) else stringResource(R.string.pull_to_refresh),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
