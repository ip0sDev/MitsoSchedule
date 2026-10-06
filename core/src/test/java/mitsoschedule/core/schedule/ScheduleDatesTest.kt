package mitsoschedule.core.schedule

import mitsoschedule.core.model.DaySchedule
import mitsoschedule.core.model.Lesson
import mitsoschedule.core.model.OptionItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleDatesTest {

    @Test
    fun testAutoRefreshPolicy() {
        // Zero timestamp -> always refresh
        assertTrue(ScheduleDates.shouldAutoRefresh(0L))

        // Fresh cache (10 seconds ago) -> should NOT refresh
        val recentMillis = System.currentTimeMillis() - 10_000L
        assertFalse(ScheduleDates.shouldAutoRefresh(recentMillis))

        // Old cache (> 8 days) -> should refresh
        val eightDaysAgo = System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000L)
        assertTrue(ScheduleDates.shouldAutoRefresh(eightDaysAgo))
    }

    @Test
    fun testIsScheduleOlderThanWeek() {
        val now = System.currentTimeMillis()
        val today = java.time.LocalDate.of(2026, 9, 24)

        // 1. Zero timestamp -> always older than week
        assertTrue(ScheduleDates.isScheduleOlderThanWeek(0L, emptyList(), today))

        // 2. Eight days ago timestamp -> true
        val eightDaysAgo = now - (8 * 24 * 60 * 60 * 1000L)
        assertTrue(ScheduleDates.isScheduleOlderThanWeek(eightDaysAgo, emptyList(), today))

        // 3. Fresh timestamp but empty schedules -> true
        assertTrue(ScheduleDates.isScheduleOlderThanWeek(now, emptyList(), today))

        // 4. Fresh timestamp with past schedule (e.g., from September 10) -> true
        val oldSchedule = listOf(
            DaySchedule(
                dayTitle = "Четверг, 10 сентября",
                lessons = listOf(Lesson(time = "08.15 — 09.35", subject = "Test", room = "101"))
            )
        )
        assertTrue(ScheduleDates.isScheduleOlderThanWeek(now, oldSchedule, today))

        // 5. Fresh timestamp with current week schedule (September 24) -> false
        val currentSchedule = listOf(
            DaySchedule(
                dayTitle = "Четверг, 24 сентября",
                lessons = listOf(Lesson(time = "08.15 — 09.35", subject = "Test", room = "101"))
            )
        )
        assertFalse(ScheduleDates.isScheduleOlderThanWeek(now, currentSchedule, today))
    }

    @Test
    fun testFindCurrentWeekId() {
        val today = java.time.LocalDate.of(2026, 9, 24) // Thursday in week 4

        val weeks = listOf(
            OptionItem(id = "1", name = "31 августа - 06 сентября"),
            OptionItem(id = "2", name = "07 сентября - 13 сентября"),
            OptionItem(id = "3", name = "14 сентября - 20 сентября"),
            OptionItem(id = "4", name = "21 сентября - 27 сентября"),
            OptionItem(id = "5", name = "28 сентября - 04 октября")
        )

        val schedules = listOf(
            DaySchedule(dayTitle = "Понедельник, 31 августа", weekId = "1", weekName = "31 августа - 06 сентября"),
            DaySchedule(dayTitle = "Вторник, 01 сентября", weekId = "1", weekName = "31 августа - 06 сентября"),
            DaySchedule(dayTitle = "Понедельник, 21 сентября", weekId = "4", weekName = "21 сентября - 27 сентября"),
            DaySchedule(dayTitle = "Четверг, 24 сентября", weekId = "4", weekName = "21 сентября - 27 сентября"),
            DaySchedule(dayTitle = "Понедельник, 28 сентября", weekId = "5", weekName = "28 сентября - 04 октября")
        )

        val currentWeekId = ScheduleDates.findCurrentWeekId(weeks, schedules, today)
        assertEquals("4", currentWeekId)
    }

    @Test
    fun testFindTodayScheduleDoesNotMatchWrongWeek() {
        val today = java.time.LocalDate.of(2026, 9, 24) // Thursday, 24 September

        // Schedule only has Thursday, 03 September from week 1
        val week1Only = listOf(
            DaySchedule(
                dayTitle = "Четверг, 03 сентября",
                lessons = listOf(Lesson(time = "08.15 — 09.35", subject = "Old Lesson"))
            )
        )

        // Must NOT match 03 сентября as today!
        val todaySched = ScheduleDates.findTodaySchedule(week1Only, today)
        assertNull(todaySched)
    }
}
