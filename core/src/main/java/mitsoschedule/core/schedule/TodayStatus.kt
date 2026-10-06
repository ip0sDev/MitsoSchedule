package mitsoschedule.core.schedule

import mitsoschedule.core.model.Lesson
import java.time.Duration
import java.time.LocalTime

enum class TodayScheduleState {
    /** Сегодня нет ни одной пары. */
    NO_LESSONS,
    NOT_STARTED,
    ONGOING_LESSON,
    BREAK_BETWEEN_LESSONS,
    FINISHED
}

/**
 * Где мы находимся в сегодняшнем дне. Только данные: формулировки собирает UI из своих ресурсов.
 *
 * @property currentLesson идущая пара (для перемены это предыдущая пара).
 * @property nextLesson следующая пара (до начала занятий это первая пара).
 * @property minutesToNext минут до [boundary] (начало первой пары, конец текущей или начало следующей).
 * @property boundary время ближайшего события или null, если время пар разобрать не удалось.
 */
data class TodayTimeInfo(
    val state: TodayScheduleState,
    val currentLesson: Lesson? = null,
    val nextLesson: Lesson? = null,
    val currentLessonIndex: Int = -1,
    val nextLessonIndex: Int = -1,
    val minutesToNext: Long = 0L,
    val boundary: LocalTime? = null
)

object TodayStatus {

    private val TIME_RANGE = Regex("""(\d{1,2})[.:](\d{2})\s*[-–—]\s*(\d{1,2})[.:](\d{2})""")

    /** "08.15 — 09.35" -> (08:15, 09:35); null, если строку разобрать не удалось. */
    fun parseTimeRange(timeStr: String?): Pair<LocalTime, LocalTime>? {
        if (timeStr.isNullOrBlank()) return null
        val match = TIME_RANGE.find(timeStr) ?: return null
        val (h1, m1, h2, m2) = match.destructured
        return try {
            Pair(LocalTime.of(h1.toInt(), m1.toInt()), LocalTime.of(h2.toInt(), m2.toInt()))
        } catch (e: Exception) {
            null
        }
    }

    fun calculate(lessons: List<Lesson>, now: LocalTime = LocalTime.now()): TodayTimeInfo {
        val active = lessons.filter { !it.isEmptyWindow }
        if (active.isEmpty()) return TodayTimeInfo(TodayScheduleState.NO_LESSONS)

        val times = active.map { parseTimeRange(it.time) }
        val firstStart = times.firstOrNull { it != null }?.first
        val lastEnd = times.lastOrNull { it != null }?.second

        if (firstStart != null && now.isBefore(firstStart)) {
            return TodayTimeInfo(
                state = TodayScheduleState.NOT_STARTED,
                nextLesson = active.first(),
                nextLessonIndex = 0,
                minutesToNext = Duration.between(now, firstStart).toMinutes(),
                boundary = firstStart
            )
        }

        if (lastEnd != null && now.isAfter(lastEnd)) {
            return TodayTimeInfo(TodayScheduleState.FINISHED)
        }

        for (i in active.indices) {
            val range = times[i] ?: continue
            if (!now.isBefore(range.first) && !now.isAfter(range.second)) {
                val next = active.getOrNull(i + 1)
                return TodayTimeInfo(
                    state = TodayScheduleState.ONGOING_LESSON,
                    currentLesson = active[i],
                    nextLesson = next,
                    currentLessonIndex = i,
                    nextLessonIndex = if (next != null) i + 1 else -1,
                    minutesToNext = Duration.between(now, range.second).toMinutes(),
                    boundary = range.second
                )
            }
        }

        for (i in 0 until active.size - 1) {
            val current = times[i]
            val next = times[i + 1]
            if (current != null && next != null && now.isAfter(current.second) && now.isBefore(next.first)) {
                return TodayTimeInfo(
                    state = TodayScheduleState.BREAK_BETWEEN_LESSONS,
                    currentLesson = active[i],
                    nextLesson = active[i + 1],
                    currentLessonIndex = i,
                    nextLessonIndex = i + 1,
                    minutesToNext = Duration.between(now, next.first).toMinutes(),
                    boundary = next.first
                )
            }
        }

        // Время пар не распознано: показываем расписание без статуса
        return TodayTimeInfo(TodayScheduleState.NOT_STARTED, nextLesson = active.first())
    }
}
