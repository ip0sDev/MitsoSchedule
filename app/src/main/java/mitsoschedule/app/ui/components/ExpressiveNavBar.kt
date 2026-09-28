package mitsoschedule.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mitsoschedule.app.ui.haptics.LocalBiolumeHaptics
import mitsoschedule.app.ui.theme.BiolumeTheme
import mitsoschedule.app.ui.theme.MitsoTestTheme
import mitsoschedule.app.ui.theme.biolumeSurface

data class ExpressiveNavItemData(
    val index: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDescription: String
)

val DefaultExpressiveNavItems = listOf(
    ExpressiveNavItemData(
        index = 0,
        label = "Расписание",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        contentDescription = "Расписание занятий"
    ),
    ExpressiveNavItemData(
        index = 1,
        label = "Кабинет",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        contentDescription = "Личный кабинет студента"
    ),
    ExpressiveNavItemData(
        index = 2,
        label = "Настройки",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        contentDescription = "Настройки приложения"
    )
)

/**
 * Dynamic Expandable Pill Navigation Dock (Material 3 Expressive & Biolume v2.1).
 *
 * Features:
 * - Dynamic Island: Selected tab expands into a horizontal pill [ Icon  Label ],
 *   while unselected tabs collapse into clean circular icon buttons [ Icon ].
 * - Expressive Motion: Fluid spring animations on expand/collapse and icon scale.
 * - Cohesive Palette: Perfect chromatic harmony with primary cyan/teal tints
 *   without awkward or mismatched slate indicators.
 * - Neumorphic Biolume surface with subtle elevation and hairline outline.
 * - Snappy tactile haptic feedback (snap) on tab switch.
 */
@Composable
fun ExpressiveFloatingNavBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    items: List<ExpressiveNavItemData> = DefaultExpressiveNavItems
) {
    val depth = BiolumeTheme.depth
    val haptics = LocalBiolumeHaptics.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .biolumeSurface(
                    shape = CircleShape,
                    tokens = depth,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
            shape = CircleShape,
            color = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentTab == item.index
                    ExpressivePillItem(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            if (currentTab != item.index) {
                                haptics.snap()
                                onTabSelected(item.index)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressivePillItem(
    item: ExpressiveNavItemData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val depth = BiolumeTheme.depth
    val interactionSource = remember { MutableInteractionSource() }

    // Chromatic harmony: clean primary glow/fill in both Abyss and Tidepool
    val activeIndicatorColor = MaterialTheme.colorScheme.primary.copy(
        alpha = if (depth.isDark) 0.22f else 0.15f
    )
    val activeContentColor = MaterialTheme.colorScheme.primary
    val inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) activeContentColor else inactiveContentColor,
        animationSpec = tween(durationMillis = 180),
        label = "pillContentColor"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.10f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillIconScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) activeIndicatorColor else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "pillBgColor"
    )

    Box(
        modifier = Modifier
            .height(46.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = MaterialTheme.colorScheme.primary
                ),
                onClick = onClick
            )
            .padding(horizontal = if (isSelected) 16.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.contentDescription,
                tint = contentColor,
                modifier = Modifier
                    .size(21.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(150, delayMillis = 60)) +
                        expandHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            expandFrom = Alignment.Start
                        ),
                exit = fadeOut(animationSpec = tween(100)) +
                       shrinkHorizontally(
                           animationSpec = spring(
                               dampingRatio = Spring.DampingRatioNoBouncy,
                               stiffness = Spring.StiffnessMedium
                           ),
                           shrinkTowards = Alignment.Start
                       )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            letterSpacing = 0.2.sp
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpressiveNavBarPreview() {
    MitsoTestTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = 24.dp)
        ) {
            ExpressiveFloatingNavBar(
                currentTab = 0,
                onTabSelected = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpressiveNavBarDarkPreview() {
    MitsoTestTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = 24.dp)
        ) {
            ExpressiveFloatingNavBar(
                currentTab = 1,
                onTabSelected = {}
            )
        }
    }
}
