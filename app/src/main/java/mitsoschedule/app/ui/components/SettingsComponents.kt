package mitsoschedule.app.ui.components

import androidx.compose.material.icons.outlined.Widgets
import mitsoschedule.app.widget.requestPinScheduleWidget
import mitsoschedule.app.ui.theme.BiolumeShapes
import androidx.compose.material3.Switch
import androidx.compose.ui.res.stringResource
import mitsoschedule.app.R
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import mitsoschedule.app.BuildConfig
import mitsoschedule.core.model.ServerHealth
import mitsoschedule.app.ui.haptics.LocalBiolumeHaptics
import mitsoschedule.app.ui.theme.BiolumeTheme
import mitsoschedule.app.ui.theme.biolumeHairline
import mitsoschedule.app.ui.theme.biolumeRaised
import mitsoschedule.app.ui.theme.biolumeSurface

@Composable
fun SettingsContent(
    serverHealth: ServerHealth?,
    isCheckingHealth: Boolean,
    lastUpdateTime: String?,
    onCheckHealth: () -> Unit,
    onClearCache: () -> Unit,
    onResetSelection: () -> Unit,
    currentTheme: String = "system",
    onThemeSelected: (String) -> Unit = {},
    useDynamicColor: Boolean = false,
    onDynamicColorChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalBiolumeHaptics.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. Theme Selection Card
        ThemeSelectionCard(
            currentTheme = currentTheme,
            onThemeSelected = onThemeSelected,
            useDynamicColor = useDynamicColor,
            onDynamicColorChange = onDynamicColorChange
        )

        // 0.5 Виджет на рабочий стол
        WidgetCard()

        // 1. App & Server Version Card
        AppAndServerVersionCard(
            serverHealth = serverHealth,
            isCheckingHealth = isCheckingHealth,
            onCheckHealth = onCheckHealth
        )

        // 2. Cache & Data Management Card
        CacheManagementCard(
            lastUpdateTime = lastUpdateTime,
            onClearCacheClick = {
                haptics.click()
                showClearCacheDialog = true
            },
            onResetSelectionClick = {
                haptics.click()
                showResetDialog = true
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
    }

    // Clear Cache Confirm Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.clear_cache_title)) },
            text = { Text(stringResource(R.string.clear_cache_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.mediumClick()
                        onClearCache()
                        showClearCacheDialog = false
                        Toast.makeText(context, context.getString(R.string.cache_cleared), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptics.tick()
                    showClearCacheDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Reset Group Selection Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_group_title)) },
            text = { Text(stringResource(R.string.reset_group_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.mediumClick()
                        onResetSelection()
                        showResetDialog = false
                        Toast.makeText(context, context.getString(R.string.group_reset_done), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.reset))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptics.tick()
                    showResetDialog = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ThemeSelectionCard(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    useDynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit
) {
    val haptics = LocalBiolumeHaptics.current
    val depth = BiolumeTheme.depth
    val cardShape = RoundedCornerShape(BiolumeShapes.CardLarge)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .biolumeRaised(shape = cardShape, tokens = depth),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                             imageVector = Icons.Outlined.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.theme_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Segmented Theme Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val themes = listOf(
                    Triple("system", stringResource(R.string.theme_auto), Icons.Outlined.BrightnessAuto),
                    Triple("dark", stringResource(R.string.theme_dark), Icons.Outlined.DarkMode),
                    Triple("light", stringResource(R.string.theme_light), Icons.Outlined.LightMode)
                )

                themes.forEach { (mode, label, icon) ->
                    val isSelected = currentTheme == mode
                    val pillShape = RoundedCornerShape(100.dp)

                    Surface(
                        shape = pillShape,
                        color = if (isSelected) depth.selectionFill else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .weight(1f)
                            .clip(pillShape)
                            .clickable {
                                if (!isSelected) {
                                    haptics.toggle()
                                    onThemeSelected(mode)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Динамические цвета (Material You)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        haptics.toggle()
                        onDynamicColorChange(!useDynamicColor)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dynamic_color_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.dynamic_color_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = useDynamicColor,
                    onCheckedChange = {
                        haptics.toggle()
                        onDynamicColorChange(it)
                    }
                )
            }
        }
    }
}

@Composable
private fun AppAndServerVersionCard(
    serverHealth: ServerHealth?,
    isCheckingHealth: Boolean,
    onCheckHealth: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val isHealthy = serverHealth?.status.equals("healthy", ignoreCase = true)
    val statusColor = when {
        isHealthy -> BiolumeTheme.status.success
        serverHealth?.status.isNullOrBlank() || isCheckingHealth -> BiolumeTheme.status.warning
        else -> MaterialTheme.colorScheme.error
    }
    val statusText = when {
        isCheckingHealth -> stringResource(R.string.status_checking)
        isHealthy -> stringResource(R.string.status_online)
        serverHealth?.status == "unreachable" -> stringResource(R.string.status_unreachable)
        serverHealth != null -> serverHealth.status
        else -> stringResource(R.string.status_unchecked)
    }

    val depth = BiolumeTheme.depth
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .biolumeRaised(shape = RoundedCornerShape(BiolumeShapes.CardLarge), tokens = depth),
        shape = RoundedCornerShape(BiolumeShapes.CardLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Dns,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.versions_status),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val haptics = LocalBiolumeHaptics.current
                IconButton(
                    onClick = {
                        haptics.click()
                        onCheckHealth()
                    },
                    enabled = !isCheckingHealth
                ) {
                    if (isCheckingHealth) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.check_status),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Version info items
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // App version
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_section),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = BiolumeTheme.dataType.dataMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Server version
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.server_section),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val serverVersion = serverHealth?.version?.takeIf { it.isNotBlank() } ?: "1.2.2"
                    Text(
                        text = "v$serverVersion",
                        style = BiolumeTheme.dataType.dataMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Server Status Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = statusColor.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.server_status, statusText),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CacheManagementCard(
    lastUpdateTime: String?,
    onClearCacheClick: () -> Unit,
    onResetSelectionClick: () -> Unit
) {
    val depth = BiolumeTheme.depth

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .biolumeRaised(shape = RoundedCornerShape(BiolumeShapes.CardLarge), tokens = depth),
        shape = RoundedCornerShape(BiolumeShapes.CardLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.storage_cache),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!lastUpdateTime.isNullOrBlank()) {
                        Text(
                            text = stringResource(R.string.updated_at, lastUpdateTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClearCacheClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(stringResource(R.string.clear_cache), maxLines = 1)
                }

                OutlinedButton(
                    onClick = onResetSelectionClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(stringResource(R.string.reset_group_button), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun WidgetCard() {
    val context = LocalContext.current
    val haptics = LocalBiolumeHaptics.current
    val depth = BiolumeTheme.depth
    val cardShape = RoundedCornerShape(BiolumeShapes.CardLarge)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .biolumeRaised(shape = cardShape, tokens = depth),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Widgets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.widget_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.widget_card_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    haptics.click()
                    if (!requestPinScheduleWidget(context)) {
                        Toast.makeText(context, context.getString(R.string.widget_pin_unsupported), Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(stringResource(R.string.widget_add), maxLines = 1)
            }
        }
    }
}
