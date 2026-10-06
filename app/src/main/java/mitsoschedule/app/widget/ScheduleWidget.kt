package mitsoschedule.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import mitsoschedule.app.AppContainer
import mitsoschedule.app.MainActivity
import mitsoschedule.app.R
import mitsoschedule.app.data.PreferencesManager
import java.time.LocalTime
import java.util.Locale

/**
 * Виджет «Ближайшая пара»: что идёт сейчас или что дальше, а на большом размере весь сегодняшний день.
 * Данные берутся из локального кэша расписания; цвета динамические (Material You).
 */
class ScheduleWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = PreferencesManager(context)
        val state = buildWidgetState(store.getSavedSelection(), store.getCachedSchedule())
        provideContent {
            GlanceTheme {
                WidgetContent(context, state)
            }
        }
    }

    private companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(250.dp, 110.dp)
        val LARGE = DpSize(250.dp, 250.dp)
    }
}

class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()
}

/** Кнопка обновления на самом виджете: тянет расписание с сервера и перерисовывает все виджеты. */
class RefreshScheduleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        AppContainer(context).refreshSchedule()
        ScheduleWidget().updateAll(context)
    }
}

/** Просит лаунчер добавить виджет на рабочий стол. false, если лаунчер этого не умеет. */
fun requestPinScheduleWidget(context: Context): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return false
    return manager.requestPinAppWidget(ComponentName(context, ScheduleWidgetReceiver::class.java), null, null)
}

@Composable
private fun WidgetContent(context: Context, state: WidgetState) {
    val size = LocalSize.current
    val showList = size.height >= 200.dp && state.lessons.isNotEmpty()
    val colors = GlanceTheme.colors

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.widgetBackground)
            .cornerRadius(28.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
    ) {
        Header(state)
        Spacer(modifier = GlanceModifier.height(8.dp))
        if (showList) {
            LessonList(context, state)
        } else {
            Focus(context, state)
        }
    }
}

@Composable
private fun Header(state: WidgetState) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = state.groupName.ifBlank { "МИТСО" },
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
        Image(
            provider = ImageProvider(R.drawable.ic_widget_refresh),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            modifier = GlanceModifier
                .size(22.dp)
                .padding(2.dp)
                .clickable(actionRunCallback<RefreshScheduleAction>())
        )
    }
}

/** Главный блок: подпись состояния и ближайшая/текущая пара. */
@Composable
private fun Focus(context: Context, state: WidgetState) {
    val colors = GlanceTheme.colors
    Text(
        text = statusLabel(context, state),
        style = TextStyle(color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold),
        maxLines = 2
    )
    val lesson = state.focus
    if (lesson != null) {
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = lesson.subject,
            style = TextStyle(color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            maxLines = 3
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        val details = listOfNotNull(lesson.time.takeIf { it.isNotBlank() }, lesson.room).joinToString(" • ")
        if (details.isNotBlank()) {
            Text(
                text = details,
                style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp),
                maxLines = 1
            )
        }
    } else {
        state.nextDay?.let { next ->
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = nextDayText(context, next),
                style = TextStyle(color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                maxLines = 3
            )
        }
    }
}

/** Крупный размер: весь сегодняшний день, текущая пара выделена. */
@Composable
private fun LessonList(context: Context, state: WidgetState) {
    val colors = GlanceTheme.colors
    Text(
        text = statusLabel(context, state),
        style = TextStyle(color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold),
        maxLines = 1
    )
    Spacer(modifier = GlanceModifier.height(6.dp))
    state.lessons.take(5).forEach { lesson ->
        val rowModifier = if (lesson.isCurrent) {
            GlanceModifier.fillMaxWidth().background(colors.primaryContainer).cornerRadius(14.dp).padding(8.dp)
        } else {
            GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp)
        }
        Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = startOf(lesson.time),
                style = TextStyle(
                    color = if (lesson.isCurrent) colors.onPrimaryContainer else colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.width(44.dp)
            )
            Text(
                text = lesson.subject,
                style = TextStyle(
                    color = if (lesson.isCurrent) colors.onPrimaryContainer else colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = if (lesson.isCurrent) FontWeight.Bold else FontWeight.Normal
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
            lesson.room?.let { room ->
                Text(
                    text = compactRoom(room),
                    style = TextStyle(
                        color = if (lesson.isCurrent) colors.onPrimaryContainer else colors.onSurfaceVariant,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
    if (state.lessons.size > 5) {
        Box(modifier = GlanceModifier.fillMaxWidth().padding(top = 2.dp)) {
            Text(
                text = context.getString(R.string.widget_more, state.lessons.size - 5),
                style = TextStyle(color = colors.onSurfaceVariant, fontSize = 11.sp)
            )
        }
    }
}

/** «ауд. 72 (к) / ауд. 73 (к)» -> «72 (к)…»: в строке списка для аудитории мало места. */
private fun compactRoom(room: String): String {
    val first = room.substringBefore(" / ").replace(Regex("""^(ауд\.|каб\.)\s*"""), "").trim()
    return if (" / " in room) "$first…" else first
}

private fun startOf(time: String) = time.substringBefore("—").trim().replace('.', ':')

private fun hhmm(time: LocalTime?): String =
    time?.let { String.format(Locale.ROOT, "%02d:%02d", it.hour, it.minute) }.orEmpty()

private fun statusLabel(context: Context, state: WidgetState): String = when (state.status) {
    WidgetStatus.NO_GROUP -> context.getString(R.string.widget_no_group)
    WidgetStatus.NO_DATA -> context.getString(R.string.widget_no_data)
    WidgetStatus.NO_LESSONS -> context.getString(R.string.widget_no_lessons)
    WidgetStatus.NOT_STARTED ->
        if (state.minutes > 0) context.getString(R.string.widget_first_in, state.minutes, hhmm(state.boundary))
        else context.getString(R.string.widget_first_at, hhmm(state.boundary))
    WidgetStatus.ONGOING -> context.getString(R.string.widget_ongoing, hhmm(state.boundary))
    WidgetStatus.BREAK -> context.getString(R.string.widget_break, hhmm(state.boundary))
    WidgetStatus.FINISHED -> context.getString(R.string.widget_finished)
}

private fun nextDayText(context: Context, next: WidgetNextDay): String {
    val first = next.first ?: return next.title
    return context.getString(R.string.widget_next_day, next.title, startOf(first.time), first.subject)
}
