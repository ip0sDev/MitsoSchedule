package mitsoschedule.core.schedule

import mitsoschedule.core.model.Lesson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class TodayStatusTest {

    private val lessons = listOf(
        Lesson(time = "08.15 — 09.35", subject = "Пара 1", room = "ауд. 312"),
        Lesson(time = "09.45 — 11.05", subject = "Пара 2", room = "ауд. 101"),
        Lesson(time = "11.20 — 12.40", subject = "Пара 3", room = "ауд. 202")
    )

    private fun at(h: Int, m: Int) = TodayStatus.calculate(lessons, LocalTime.of(h, m))

    @Test
    fun beforeFirstLesson() {
        val info = at(7, 30)
        assertEquals(TodayScheduleState.NOT_STARTED, info.state)
        assertEquals("Пара 1", info.nextLesson?.subject)
        assertEquals(0, info.nextLessonIndex)
        assertEquals(45L, info.minutesToNext)
        assertEquals(LocalTime.of(8, 15), info.boundary)
        assertNull(info.currentLesson)
    }

    @Test
    fun duringLessonKnowsCurrentNextAndEnd() {
        val info = at(8, 30)
        assertEquals(TodayScheduleState.ONGOING_LESSON, info.state)
        assertEquals("Пара 1", info.currentLesson?.subject)
        assertEquals("ауд. 312", info.currentLesson?.room)
        assertEquals("Пара 2", info.nextLesson?.subject)
        assertEquals(0, info.currentLessonIndex)
        assertEquals(1, info.nextLessonIndex)
        assertEquals(65L, info.minutesToNext)
        assertEquals(LocalTime.of(9, 35), info.boundary)
    }

    @Test
    fun lastLessonHasNoNext() {
        val info = at(12, 0)
        assertEquals(TodayScheduleState.ONGOING_LESSON, info.state)
        assertNull(info.nextLesson)
        assertEquals(-1, info.nextLessonIndex)
    }

    @Test
    fun betweenLessons() {
        val info = at(9, 40)
        assertEquals(TodayScheduleState.BREAK_BETWEEN_LESSONS, info.state)
        assertEquals("Пара 1", info.currentLesson?.subject)
        assertEquals("Пара 2", info.nextLesson?.subject)
        assertEquals(5L, info.minutesToNext)
        assertEquals(LocalTime.of(9, 45), info.boundary)
    }

    @Test
    fun afterLastLesson() {
        assertEquals(TodayScheduleState.FINISHED, at(13, 0).state)
    }

    @Test
    fun emptyWindowsDoNotCount() {
        val onlyWindows = listOf(Lesson(time = "08.15 — 09.35", subject = "Нет занятий", isEmptyWindow = true))
        assertEquals(TodayScheduleState.NO_LESSONS, TodayStatus.calculate(onlyWindows, LocalTime.of(9, 0)).state)
        assertEquals(TodayScheduleState.NO_LESSONS, TodayStatus.calculate(emptyList(), LocalTime.of(9, 0)).state)

        // окно между парами игнорируется: индексы считаются только по реальным парам
        val withWindow = listOf(
            Lesson(time = "08.15 — 09.35", subject = "Пара 1"),
            Lesson(time = "09.45 — 11.05", subject = "Нет занятий", isEmptyWindow = true),
            Lesson(time = "11.20 — 12.40", subject = "Пара 3")
        )
        val info = TodayStatus.calculate(withWindow, LocalTime.of(10, 0))
        assertEquals(TodayScheduleState.BREAK_BETWEEN_LESSONS, info.state)
        assertEquals("Пара 3", info.nextLesson?.subject)
    }

    @Test
    fun unparsableTimesFallBackToPlainSchedule() {
        val info = TodayStatus.calculate(listOf(Lesson(time = "утром", subject = "Пара")), LocalTime.of(9, 0))
        assertEquals(TodayScheduleState.NOT_STARTED, info.state)
        assertNull(info.boundary)
        assertEquals("Пара", info.nextLesson?.subject)
    }

    @Test
    fun parseTimeRangeAcceptsKnownFormats() {
        assertEquals(LocalTime.of(8, 15) to LocalTime.of(9, 35), TodayStatus.parseTimeRange("08.15 — 09.35"))
        assertEquals(LocalTime.of(9, 45) to LocalTime.of(11, 5), TodayStatus.parseTimeRange("9.45-11.05"))
        assertEquals(LocalTime.of(14, 40) to LocalTime.of(16, 5), TodayStatus.parseTimeRange("14:40 – 16:05"))
        assertNull(TodayStatus.parseTimeRange(null))
        assertNull(TodayStatus.parseTimeRange("25.00 — 26.00"))
    }
}
