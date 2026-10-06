package mitsoschedule.app

import mitsoschedule.app.ui.components.messages
import mitsoschedule.core.model.Lesson
import mitsoschedule.core.schedule.TodayStatus
import mitsoschedule.core.ui.UiStrings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class TodayMessagesTest {

    /** «<id ресурса>:<аргументы>»: проверяем, какой ресурс и с какими аргументами выбран. */
    private object FakeStrings : UiStrings {
        override fun get(id: Int, vararg args: Any) = "$id:${args.joinToString(",")}"
    }

    private fun s(id: Int, vararg args: Any) = FakeStrings.get(id, *args)

    private val lessons = listOf(
        Lesson(time = "08.15 — 09.35", subject = "Пара 1"),
        Lesson(time = "09.45 — 11.05", subject = "Пара 2"),
        Lesson(time = "14.40 — 16.05", subject = "Пара 3")
    )

    private fun messagesAt(h: Int, m: Int) = TodayStatus.calculate(lessons, LocalTime.of(h, m)).messages(FakeStrings)

    @Test
    fun beforeClasses() {
        assertEquals(s(R.string.today_not_started) to s(R.string.today_first_at, "08:15"), messagesAt(7, 30))
    }

    @Test
    fun duringClassShowsNumberAndEnd() {
        assertEquals(s(R.string.today_ongoing, 1) to s(R.string.today_ends_at, "09:35"), messagesAt(8, 30))
    }

    @Test
    fun breakShowsMinutesAndNextStart() {
        assertEquals(s(R.string.today_break) to s(R.string.today_next_in, 5L, "09:45"), messagesAt(9, 40))
        // меньше минуты до начала: без «через N мин»
        val almost = TodayStatus.calculate(lessons, LocalTime.of(9, 44, 30)).messages(FakeStrings)
        assertEquals(s(R.string.today_break) to s(R.string.today_next_at, "09:45"), almost)
    }

    @Test
    fun afterClasses() {
        assertEquals(s(R.string.today_finished) to s(R.string.enjoy_rest), messagesAt(17, 0))
    }

    @Test
    fun noLessons() {
        val info = TodayStatus.calculate(emptyList(), LocalTime.of(9, 0))
        assertEquals(s(R.string.day_no_lessons) to "", info.messages(FakeStrings))
    }
}
