package mitsoschedule.app.ui.components

import mitsoschedule.app.R
import mitsoschedule.core.schedule.TodayScheduleState
import mitsoschedule.core.schedule.TodayTimeInfo
import mitsoschedule.core.ui.UiStrings
import java.time.LocalTime
import java.util.Locale

private fun LocalTime.hhmm() = String.format(Locale.ROOT, "%02d:%02d", hour, minute)

/** Заголовок и пояснение для баннера «сегодня». Пояснение может быть пустым. */
fun TodayTimeInfo.messages(strings: UiStrings): Pair<String, String> = when (state) {
    TodayScheduleState.NO_LESSONS -> strings.get(R.string.day_no_lessons) to ""
    TodayScheduleState.NOT_STARTED -> {
        val start = boundary
        if (start == null) "" to ""
        else strings.get(R.string.today_not_started) to strings.get(R.string.today_first_at, start.hhmm())
    }
    TodayScheduleState.FINISHED -> strings.get(R.string.today_finished) to strings.get(R.string.enjoy_rest)
    TodayScheduleState.ONGOING_LESSON ->
        strings.get(R.string.today_ongoing, currentLessonIndex + 1) to
            strings.get(R.string.today_ends_at, boundary?.hhmm().orEmpty())
    TodayScheduleState.BREAK_BETWEEN_LESSONS -> {
        val start = boundary?.hhmm().orEmpty()
        strings.get(R.string.today_break) to
            if (minutesToNext > 0) strings.get(R.string.today_next_in, minutesToNext, start)
            else strings.get(R.string.today_next_at, start)
    }
}
