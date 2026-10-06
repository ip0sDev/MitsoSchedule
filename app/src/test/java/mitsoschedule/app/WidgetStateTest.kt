package mitsoschedule.app

import mitsoschedule.app.widget.WidgetStatus
import mitsoschedule.app.widget.buildWidgetState
import mitsoschedule.core.model.DaySchedule
import mitsoschedule.core.model.Lesson
import mitsoschedule.core.model.UserSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class WidgetStateTest {

    private val selection = UserSelection(facultyId = "f", courseId = "c", groupId = "g", groupName = "2423 УИР")

    private fun lesson(time: String, subject: String, room: String? = null) =
        Lesson(time = time, subject = subject, room = room)

    private val schedules = listOf(
        DaySchedule(
            dayTitle = "Вторник, 06 октября",
            lessons = listOf(
                lesson("08.15 — 09.35", "Пара 1", "ауд. 61"),
                lesson("09.45 — 11.05", "Пара 2", "ауд. 62")
            )
        ),
        DaySchedule(
            dayTitle = "Среда, 07 октября",
            lessons = listOf(lesson("11.15 — 12.35", "Пара утром завтра", "ауд. 5"))
        )
    )

    private fun at(h: Int, m: Int) = LocalDateTime.of(2026, 10, 6, h, m)

    @Test
    fun withoutGroupOrDataShowsPlaceholders() {
        assertEquals(WidgetStatus.NO_GROUP, buildWidgetState(null, schedules, at(9, 0)).status)
        assertEquals(WidgetStatus.NO_GROUP, buildWidgetState(UserSelection(), schedules, at(9, 0)).status)
        assertEquals(WidgetStatus.NO_DATA, buildWidgetState(selection, emptyList(), at(9, 0)).status)
    }

    @Test
    fun beforeClassesFocusesOnFirstLesson() {
        val state = buildWidgetState(selection, schedules, at(7, 30))
        assertEquals(WidgetStatus.NOT_STARTED, state.status)
        assertEquals("2423 УИР", state.groupName)
        assertEquals("Пара 1", state.focus?.subject)
        assertEquals(45L, state.minutes)
        assertEquals(LocalTime.of(8, 15), state.boundary)
        assertEquals(2, state.lessons.size)
    }

    @Test
    fun duringLessonMarksItCurrent() {
        val state = buildWidgetState(selection, schedules, at(8, 30))
        assertEquals(WidgetStatus.ONGOING, state.status)
        assertEquals("Пара 1", state.focus?.subject)
        assertEquals("ауд. 61", state.focus?.room)
        assertEquals(65L, state.minutes)
        assertTrue(state.lessons[0].isCurrent)
        assertFalse(state.lessons[1].isCurrent)
    }

    @Test
    fun breakFocusesOnNextLesson() {
        val state = buildWidgetState(selection, schedules, at(9, 40))
        assertEquals(WidgetStatus.BREAK, state.status)
        assertEquals("Пара 2", state.focus?.subject)
        assertEquals(5L, state.minutes)
    }

    @Test
    fun afterClassesPointsToNextDay() {
        val state = buildWidgetState(selection, schedules, at(15, 0))
        assertEquals(WidgetStatus.FINISHED, state.status)
        assertEquals("Среда, 07 октября", state.nextDay?.title)
        assertEquals("Пара утром завтра", state.nextDay?.first?.subject)
    }

    @Test
    fun dayWithoutScheduleIsNoLessonsAndKeepsNextDay() {
        val saturday = LocalDateTime.of(2026, 10, 10, 10, 0)
        val state = buildWidgetState(selection, schedules, saturday)
        assertEquals(WidgetStatus.NO_LESSONS, state.status)
        assertNull(state.nextDay)

        val monday = LocalDateTime.of(2026, 10, 5, 10, 0)
        val before = buildWidgetState(selection, schedules, monday)
        assertEquals(WidgetStatus.NO_LESSONS, before.status)
        assertEquals("Вторник, 06 октября", before.nextDay?.title)
    }

    @Test
    fun emptyWindowsAreSkippedAndSubgroupRoomsJoined() {
        val day = DaySchedule(
            dayTitle = "Вторник, 06 октября",
            lessons = listOf(
                Lesson(time = "08.15 — 09.35", subject = "Нет занятий", isEmptyWindow = true),
                Lesson(
                    time = "09.45 — 11.05",
                    subject = "Лаба",
                    subgroups = listOf(
                        mitsoschedule.core.model.SubgroupInfo(subgroup = "1 подгруппа", room = "ауд. 72 (к)"),
                        mitsoschedule.core.model.SubgroupInfo(subgroup = "2 подгруппа", room = "ауд. 73 (к)")
                    )
                )
            )
        )
        val state = buildWidgetState(selection, listOf(day), at(7, 0))
        assertEquals(1, state.lessons.size)
        assertEquals("Лаба", state.focus?.subject)
        assertEquals("ауд. 72 (к) / ауд. 73 (к)", state.focus?.room)
    }
}
