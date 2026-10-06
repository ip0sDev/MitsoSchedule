package by.iposdev.watchso

import by.iposdev.watchso.presentation.components.messages
import mitsoschedule.core.model.Lesson
import mitsoschedule.core.schedule.TodayStatus
import mitsoschedule.core.ui.UiStrings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class WearTodayMessagesTest {

    private object FakeStrings : UiStrings {
        override fun get(id: Int, vararg args: Any) = "$id:${args.joinToString(",")}"
    }

    private fun s(id: Int, vararg args: Any) = FakeStrings.get(id, *args)

    private val lessons = listOf(
        Lesson(time = "08.15 — 09.35", subject = "Пара 1"),
        Lesson(time = "09.45 — 11.05", subject = "Пара 2")
    )

    private fun messagesAt(h: Int, m: Int) = TodayStatus.calculate(lessons, LocalTime.of(h, m)).messages(FakeStrings)

    @Test
    fun beforeClassesCountsMinutes() {
        assertEquals(s(R.string.today_not_started) to s(R.string.today_first_in, 45L, "08:15"), messagesAt(7, 30))
    }

    @Test
    fun duringClassShowsEndAndMinutesLeft() {
        assertEquals(s(R.string.today_ongoing, 1) to s(R.string.today_until, "09:35", 65L), messagesAt(8, 30))
    }

    @Test
    fun breakAndFinished() {
        assertEquals(s(R.string.today_break) to s(R.string.today_next_in, 5L, "09:45"), messagesAt(9, 40))
        assertEquals(s(R.string.today_done) to s(R.string.enjoy_rest), messagesAt(13, 0))
    }

    @Test
    fun noLessonsAndUnparsableTimes() {
        val none = TodayStatus.calculate(emptyList(), LocalTime.of(9, 0)).messages(FakeStrings)
        assertEquals(s(R.string.today_none) to s(R.string.enjoy_day), none)

        val unknown = TodayStatus.calculate(listOf(Lesson(time = "утром", subject = "Пара")), LocalTime.of(9, 0))
        assertEquals(s(R.string.today_schedule) to s(R.string.today_planned), unknown.messages(FakeStrings))
    }
}
