package by.iposdev.watchso.tile

import mitsoschedule.core.schedule.TodayStatus
import by.iposdev.watchso.R
import mitsoschedule.core.schedule.ScheduleDates
import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.material.ChipColors
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import mitsoschedule.core.model.DaySchedule
import mitsoschedule.core.schedule.TodayScheduleState
import mitsoschedule.core.schedule.TodayTimeInfo
import mitsoschedule.core.model.UserSelection
import by.iposdev.watchso.data.WatchPreferencesManager
import by.iposdev.watchso.data.createWatchWebWorker
import mitsoschedule.core.network.orEmpty
import by.iposdev.watchso.presentation.MainActivity
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ScheduleTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Biolume Color Palette Constants for Tile
    companion object {
        private const val COLOR_CYAN = 0xFF35C7E8.toInt()
        private const val COLOR_ON_CYAN = 0xFF00212B.toInt()
        private const val COLOR_BG_CARD = 0xFF141C22.toInt()
        private const val COLOR_TEXT_PRIMARY = 0xFFECEFEE.toInt()
        private const val COLOR_TEXT_SECONDARY = 0xFF93A0A0.toInt()
        private const val COLOR_PILL_CYAN = 0x3335C7E8.toInt()
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return CallbackToFutureAdapter.getFuture { completer ->
            serviceScope.launch {
                try {
                    val tile = buildTile(requestParams)
                    completer.set(tile)
                } catch (e: Exception) {
                    completer.setException(e)
                }
            }
        }
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return CallbackToFutureAdapter.getFuture { completer ->
            completer.set(
                ResourceBuilders.Resources.Builder()
                    .setVersion("1")
                    .build()
            )
        }
    }

    private suspend fun buildTile(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        val prefs = WatchPreferencesManager(applicationContext)
        val selection = prefs.getSavedSelection()
        val schedules = prefs.getCachedSchedule()
        val deviceParams = requestParams.deviceConfiguration

        if (selection != null && selection.isComplete && ScheduleDates.isScheduleOlderThanWeek(prefs.getLastFetchMillis(), schedules)) {
            serviceScope.launch {
                try {
                    val webWorker = createWatchWebWorker()
                    val fresh = webWorker.fetchScheduleForWeeks(selection, emptyList()).orEmpty()
                    if (fresh.isNotEmpty()) {
                        val updateTime = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                        prefs.saveSchedule(fresh, updateTime)
                        getUpdater(applicationContext).requestUpdate(ScheduleTileService::class.java)
                    }
                } catch (_: Exception) {}
            }
        }

        val launchAction = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(packageName)
                    .setClassName(MainActivity::class.java.name)
                    .build()
            )
            .build()

        val tileClickable = ModifiersBuilders.Clickable.Builder()
            .setId("tile_launch_app")
            .setOnClick(launchAction)
            .build()

        val layoutElement = if (selection == null || !selection.isComplete) {
            buildNoSelectionLayout(deviceParams, tileClickable)
        } else {
            val todaySched = ScheduleDates.findTodaySchedule(schedules)
            val timeInfo = todaySched?.let { TodayStatus.calculate(it.lessons) }
            buildScheduleLayout(selection, todaySched, timeInfo, deviceParams, tileClickable)
        }

        val timeline = androidx.wear.protolayout.TimelineBuilders.Timeline.fromLayoutElement(layoutElement)

        return TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setTileTimeline(timeline)
            .setFreshnessIntervalMillis(60 * 1000L)
            .build()
    }

    private fun buildNoSelectionLayout(
        deviceParams: DeviceParametersBuilders.DeviceParameters,
        clickable: ModifiersBuilders.Clickable
    ): LayoutElementBuilders.LayoutElement {
        val content = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(
                Text.Builder(applicationContext, applicationContext.getString(R.string.mitso))
                    .setTypography(Typography.TYPOGRAPHY_TITLE2)
                    .setColor(ColorBuilders.argb(COLOR_CYAN))
                    .build()
            )
            .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(6f)).build())
            .addContent(
                Text.Builder(applicationContext, applicationContext.getString(R.string.tile_choose_group))
                    .setTypography(Typography.TYPOGRAPHY_BODY2)
                    .setColor(ColorBuilders.argb(COLOR_TEXT_SECONDARY))
                    .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
                    .build()
            )
            .build()

        val chipColors = ChipColors.primaryChipColors(
            Colors(
                COLOR_CYAN,
                COLOR_ON_CYAN,
                COLOR_CYAN,
                COLOR_ON_CYAN
            )
        )

        val primaryLayout = PrimaryLayout.Builder(deviceParams)
            .setContent(content)
            .setPrimaryChipContent(
                CompactChip.Builder(applicationContext, applicationContext.getString(R.string.open), clickable, deviceParams)
                    .setChipColors(chipColors)
                    .build()
            )
            .build()

        return LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(clickable).build())
            .addContent(primaryLayout)
            .build()
    }

    private fun buildScheduleLayout(
        selection: UserSelection,
        todaySched: DaySchedule?,
        timeInfo: TodayTimeInfo?,
        deviceParams: DeviceParametersBuilders.DeviceParameters,
        clickable: ModifiersBuilders.Clickable
    ): LayoutElementBuilders.LayoutElement {
        val headerTitle = selection.groupName.ifBlank { applicationContext.getString(R.string.mitso) }

        val column = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setWidth(DimensionBuilders.expand())

        // Top group tag
        column.addContent(
            Text.Builder(applicationContext, headerTitle)
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .setColor(ColorBuilders.argb(COLOR_CYAN))
                .build()
        )
        column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(3f)).build())

        if (todaySched == null || todaySched.lessons.isEmpty() || timeInfo == null || timeInfo.state == TodayScheduleState.NO_LESSONS) {
            column.addContent(
                Text.Builder(applicationContext, applicationContext.getString(R.string.no_lessons_today))
                    .setTypography(Typography.TYPOGRAPHY_TITLE3)
                    .setColor(ColorBuilders.argb(COLOR_TEXT_PRIMARY))
                    .build()
            )
            column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4f)).build())
            column.addContent(
                Text.Builder(applicationContext, applicationContext.getString(R.string.enjoy_rest))
                    .setTypography(Typography.TYPOGRAPHY_BODY2)
                    .setColor(ColorBuilders.argb(COLOR_TEXT_SECONDARY))
                    .build()
            )
        } else {
            when (timeInfo.state) {
                TodayScheduleState.ONGOING_LESSON -> {
                    val lesson = timeInfo.currentLesson
                    // Status Pill
                    column.addContent(
                        buildStatusPill(applicationContext, applicationContext.getString(R.string.tile_now), COLOR_CYAN, COLOR_PILL_CYAN)
                    )
                    column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4f)).build())

                    // Lesson Subject
                    column.addContent(
                        Text.Builder(applicationContext, lesson?.subject ?: applicationContext.getString(R.string.lesson))
                            .setTypography(Typography.TYPOGRAPHY_TITLE3)
                            .setColor(ColorBuilders.argb(COLOR_TEXT_PRIMARY))
                            .setMaxLines(2)
                            .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
                            .build()
                    )
                    column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4f)).build())

                    // Room & Time bottom row
                    val roomText = lesson?.room ?: ""
                    val timeText = lesson?.time ?: ""
                    val infoText = if (roomText.isNotBlank()) "$roomText • $timeText" else timeText
                    column.addContent(
                        Text.Builder(applicationContext, infoText)
                            .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                            .setColor(ColorBuilders.argb(COLOR_CYAN))
                            .build()
                    )
                }

                TodayScheduleState.BREAK_BETWEEN_LESSONS -> {
                    val next = timeInfo.nextLesson
                    val minutesLeft = timeInfo.minutesToNext
                    val pillText = if (minutesLeft > 0) applicationContext.getString(R.string.tile_break_next, minutesLeft) else applicationContext.getString(R.string.tile_break)
                    column.addContent(
                        buildStatusPill(applicationContext, pillText, 0xFFFFC24E.toInt(), 0x33FFC24E.toInt())
                    )
                    column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4f)).build())

                    column.addContent(
                        Text.Builder(applicationContext, next?.subject ?: applicationContext.getString(R.string.next_lesson))
                            .setTypography(Typography.TYPOGRAPHY_TITLE3)
                            .setColor(ColorBuilders.argb(COLOR_TEXT_PRIMARY))
                            .setMaxLines(2)
                            .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
                            .build()
                    )
                    column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4f)).build())

                    val roomText = next?.room ?: ""
                    val startText = next?.time?.substringBefore("—")?.trim() ?: ""
                    val subInfo = if (roomText.isNotBlank()) applicationContext.getString(R.string.tile_at_room, startText, roomText) else applicationContext.getString(R.string.tile_at, startText)
                    column.addContent(
                        Text.Builder(applicationContext, subInfo)
                            .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                            .setColor(ColorBuilders.argb(COLOR_TEXT_SECONDARY))
                            .build()
                    )
                }

                TodayScheduleState.NOT_STARTED -> {
                    val first = timeInfo.nextLesson
                    val startTime = first?.time?.substringBefore("—")?.trim() ?: ""
                    column.addContent(
                        buildStatusPill(applicationContext, applicationContext.getString(R.string.tile_first_at, startTime), COLOR_CYAN, COLOR_PILL_CYAN)
                    )
                    column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4f)).build())

                    column.addContent(
                        Text.Builder(applicationContext, first?.subject ?: applicationContext.getString(R.string.lesson_generic))
                            .setTypography(Typography.TYPOGRAPHY_TITLE3)
                            .setColor(ColorBuilders.argb(COLOR_TEXT_PRIMARY))
                            .setMaxLines(2)
                            .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
                            .build()
                    )
                    column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4f)).build())

                    val roomText = first?.room ?: ""
                    if (roomText.isNotBlank()) {
                        column.addContent(
                            Text.Builder(applicationContext, roomText)
                                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                                .setColor(ColorBuilders.argb(COLOR_CYAN))
                                .build()
                        )
                    }
                }

                TodayScheduleState.FINISHED -> {
                    column.addContent(
                        buildStatusPill(applicationContext, applicationContext.getString(R.string.tile_all_done), COLOR_TEXT_SECONDARY, COLOR_BG_CARD)
                    )
                    column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(6f)).build())
                    column.addContent(
                        Text.Builder(applicationContext, applicationContext.getString(R.string.lessons_finished))
                            .setTypography(Typography.TYPOGRAPHY_TITLE3)
                            .setColor(ColorBuilders.argb(COLOR_TEXT_PRIMARY))
                            .build()
                    )
                    column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(4f)).build())
                    column.addContent(
                        Text.Builder(applicationContext, applicationContext.getString(R.string.enjoy_rest))
                            .setTypography(Typography.TYPOGRAPHY_BODY2)
                            .setColor(ColorBuilders.argb(COLOR_TEXT_SECONDARY))
                            .build()
                    )
                }

                TodayScheduleState.NO_LESSONS -> {}
            }
        }

        val chipColors = ChipColors.primaryChipColors(
            Colors(
                COLOR_CYAN,
                COLOR_ON_CYAN,
                COLOR_CYAN,
                COLOR_ON_CYAN
            )
        )

        val primaryLayout = PrimaryLayout.Builder(deviceParams)
            .setContent(column.build())
            .setPrimaryChipContent(
                CompactChip.Builder(applicationContext, applicationContext.getString(R.string.schedule), clickable, deviceParams)
                    .setChipColors(chipColors)
                    .build()
            )
            .build()

        return LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(clickable)
                    .build()
            )
            .addContent(primaryLayout)
            .build()
    }

    private fun buildStatusPill(
        context: Context,
        text: String,
        textColor: Int,
        bgColor: Int
    ): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Box.Builder()
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(ColorBuilders.argb(bgColor))
                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(DimensionBuilders.dp(100f)).build())
                            .build()
                    )
                    .setPadding(
                        ModifiersBuilders.Padding.Builder()
                            .setStart(DimensionBuilders.dp(8f))
                            .setEnd(DimensionBuilders.dp(8f))
                            .setTop(DimensionBuilders.dp(2f))
                            .setBottom(DimensionBuilders.dp(2f))
                            .build()
                    )
                    .build()
            )
            .addContent(
                Text.Builder(context, text)
                    .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                    .setColor(ColorBuilders.argb(textColor))
                    .build()
            )
            .build()
    }
}
