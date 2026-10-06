package mitsoschedule.app.widget

import mitsoschedule.core.model.DaySchedule
import mitsoschedule.core.model.Lesson
import mitsoschedule.core.model.UserSelection
import mitsoschedule.core.schedule.ScheduleDates
import mitsoschedule.core.schedule.ScheduleNormalizer
import mitsoschedule.core.schedule.TodayScheduleState
import mitsoschedule.core.schedule.TodayStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class WidgetStatus {
    NO_GROUP,
    NO_DATA,
    NO_LESSONS,
    NOT_STARTED,
    ONGOING,
    BREAK,
    FINISHED
}

data class WidgetLesson(
    val time: String,
    val subject: String,
    val room: String?,
    val isCurrent: Boolean = false
)

/** Ближайший день с парами после сегодняшнего: показывается, когда на сегодня всё закончилось. */
data class WidgetNextDay(val title: String, val first: WidgetLesson?)

/**
 * Всё, что нужно виджету, в виде данных. Тексты собирает UI из ресурсов (как баннер «сегодня»),
 * а здесь только логика, чтобы её можно было проверить тестами.
 *
 * @property focus идущая пара, а до начала занятий и на перемене ближайшая.
 * @property minutes минут до [boundary] (начала первой пары, конца текущей или начала следующей).
 * @property lessons пары сегодняшнего дня по порядку.
 */
data class WidgetState(
    val status: WidgetStatus,
    val groupName: String = "",
    val focus: WidgetLesson? = null,
    val minutes: Long = 0L,
    val boundary: LocalTime? = null,
    val lessons: List<WidgetLesson> = emptyList(),
    val nextDay: WidgetNextDay? = null
)

fun buildWidgetState(
    selection: UserSelection?,
    schedules: List<DaySchedule>,
    now: LocalDateTime = LocalDateTime.now()
): WidgetState {
    if (selection == null || !selection.isComplete) return WidgetState(WidgetStatus.NO_GROUP)
    val group = selection.groupName
    if (schedules.isEmpty()) return WidgetState(WidgetStatus.NO_DATA, groupName = group)

    val today = now.toLocalDate()
    val todayLessons = ScheduleDates.findTodaySchedule(schedules, today)?.let(::activeLessons).orEmpty()
    if (todayLessons.isEmpty()) {
        return WidgetState(WidgetStatus.NO_LESSONS, groupName = group, nextDay = nextDayWithLessons(schedules, today))
    }

    val info = TodayStatus.calculate(todayLessons, now.toLocalTime())
    val currentIndex = info.currentLessonIndex.takeIf { info.state == TodayScheduleState.ONGOING_LESSON } ?: -1
    val list = todayLessons.mapIndexed { i, lesson -> lesson.toWidget(isCurrent = i == currentIndex) }

    return when (info.state) {
        TodayScheduleState.FINISHED -> WidgetState(
            WidgetStatus.FINISHED, group, lessons = list, nextDay = nextDayWithLessons(schedules, today)
        )
        TodayScheduleState.NO_LESSONS -> WidgetState(
            WidgetStatus.NO_LESSONS, group, nextDay = nextDayWithLessons(schedules, today)
        )
        TodayScheduleState.ONGOING_LESSON -> WidgetState(
            WidgetStatus.ONGOING, group, info.currentLesson?.toWidget(true), info.minutesToNext, info.boundary, list
        )
        TodayScheduleState.BREAK_BETWEEN_LESSONS -> WidgetState(
            WidgetStatus.BREAK, group, info.nextLesson?.toWidget(), info.minutesToNext, info.boundary, list
        )
        TodayScheduleState.NOT_STARTED -> WidgetState(
            WidgetStatus.NOT_STARTED, group, info.nextLesson?.toWidget(), info.minutesToNext, info.boundary, list
        )
    }
}

/** Пары дня в том же виде, что видит пользователь в приложении: очищенные и слитые по подгруппам. */
private fun activeLessons(day: DaySchedule): List<Lesson> =
    ScheduleNormalizer.groupLessonsByTime(day.lessons.map { ScheduleNormalizer.normalizeLesson(it) })
        .filter { !it.isEmptyWindow }

private fun nextDayWithLessons(schedules: List<DaySchedule>, today: LocalDate): WidgetNextDay? =
    schedules
        .mapNotNull { day -> ScheduleDates.parseDate(day, today)?.takeIf { it.isAfter(today) }?.let { it to day } }
        .sortedBy { it.first }
        .firstNotNullOfOrNull { (_, day) ->
            val lessons = activeLessons(day)
            if (lessons.isEmpty()) null else WidgetNextDay(day.dayTitle, lessons.first().toWidget())
        }

private fun Lesson.toWidget(isCurrent: Boolean = false) = WidgetLesson(
    time = time.orEmpty(),
    subject = subject,
    room = room?.takeIf { it.isNotBlank() }
        ?: subgroups.mapNotNull { it.room?.takeIf { r -> r.isNotBlank() } }.distinct().joinToString(" / ").ifBlank { null },
    isCurrent = isCurrent
)
