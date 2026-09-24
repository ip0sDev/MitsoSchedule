package mitsoschedule.app.ui.components

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
import mitsoschedule.app.data.ServerHealth
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
            onThemeSelected = onThemeSelected
        )

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
            title = { Text("Очистить кэш расписания?") },
            text = { Text("Сохранённое на устройстве расписание будет удалено. При следующем запуске оно загрузится заново с сервера.") },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.mediumClick()
                        onClearCache()
                        showClearCacheDialog = false
                        Toast.makeText(context, "Кэш очищен", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptics.tick()
                    showClearCacheDialog = false
                }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Reset Group Selection Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Сбросить выбранную группу?") },
            text = { Text("Выбор группы и факультета будет сброшен, потребуется выбрать их повторно.") },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.mediumClick()
                        onResetSelection()
                        showResetDialog = false
                        Toast.makeText(context, "Выбор группы сброшен", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Сбросить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptics.tick()
                    showResetDialog = false
                }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun ThemeSelectionCard(
    currentTheme: String,
    onThemeSelected: (String) -> Unit
) {
    val haptics = LocalBiolumeHaptics.current
    val depth = BiolumeTheme.depth
    val cardShape = RoundedCornerShape(24.dp)

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
                    text = "Тема оформления",
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
                    Triple("system", "Авто", Icons.Outlined.BrightnessAuto),
                    Triple("dark", "Тёмная", Icons.Outlined.DarkMode),
                    Triple("light", "Светлая", Icons.Outlined.LightMode)
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
        isCheckingHealth -> "Проверка..."
        isHealthy -> "В сети"
        serverHealth?.status == "unreachable" -> "Недоступен"
        serverHealth != null -> serverHealth.status
        else -> "Не проверен"
    }

    val depth = BiolumeTheme.depth
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .biolumeRaised(shape = RoundedCornerShape(24.dp), tokens = depth),
        shape = RoundedCornerShape(24.dp),
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
                            text = "Версии и статус",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "МИТСО Расписание",
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
                            contentDescription = "Проверить статус",
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
                        text = "Приложение",
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
                        text = "Сервер",
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
                        text = "Статус сервера: $statusText",
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
            .biolumeRaised(shape = RoundedCornerShape(24.dp), tokens = depth),
        shape = RoundedCornerShape(24.dp),
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
                        text = "Хранилище и кэш",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!lastUpdateTime.isNullOrBlank()) {
                        Text(
                            text = "Обновлено: $lastUpdateTime",
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
                    Text("Очистить кэш", maxLines = 1)
                }

                OutlinedButton(
                    onClick = onResetSelectionClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("Сброс группы", maxLines = 1)
                }
            }
        }
    }
}
