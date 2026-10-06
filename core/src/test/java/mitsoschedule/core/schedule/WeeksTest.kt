package mitsoschedule.core.schedule

import mitsoschedule.core.model.DaySchedule
import mitsoschedule.core.model.OptionItem
import mitsoschedule.core.model.UserSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeeksTest {

    private val today = LocalDate.of(2026, 9, 10)

    private val weeks = listOf(
        OptionItem("1", "31 августа - 06 сентября"),
        OptionItem("2", "07 сентября - 13 сентября"),
        OptionItem("3", "14 сентября - 20 сентября")
    )

    @Test
    fun allOptionIsAddedOnlyForTwoOrMoreWeeks() {
        assertEquals(Weeks.ALL_ID, Weeks.withAllOption(weeks).first().id)
        assertEquals(4, Weeks.withAllOption(weeks).size)
        assertEquals(1, Weeks.withAllOption(weeks.take(1)).size)
        // повторно не добавляется
        assertEquals(4, Weeks.withAllOption(Weeks.withAllOption(weeks)).size)
    }

    @Test
    fun navigationSkipsVirtualAllOption() {
        val list = Weeks.withAllOption(weeks)
        assertFalse(Weeks.hasPrevious(list, "1"))
        assertTrue(Weeks.hasNext(list, "1"))
        assertEquals("2", Weeks.next(list, "1")?.id)
        assertEquals("1", Weeks.previous(list, "2")?.id)
        assertNull(Weeks.next(list, "3"))
        // неизвестная неделя и «все недели» не навигируются
        assertFalse(Weeks.hasNext(list, Weeks.ALL_ID))
        assertFalse(Weeks.hasPrevious(list, "unknown"))
    }

    @Test
    fun fromScheduleDropsPlaceholdersAndDuplicates() {
        val days = listOf(
            DaySchedule(dayTitle = "a", weekId = "1", weekName = "w1"),
            DaySchedule(dayTitle = "b", weekId = "1", weekName = "w1"),
            DaySchedule(dayTitle = "c", weekId = "0"),
            DaySchedule(dayTitle = "d", weekId = "")
        )
        assertEquals(listOf(OptionItem("1", "w1")), Weeks.fromSchedule(days))
    }

    @Test
    fun resolveStoredReplacesPlaceholderWeekWithCurrent() {
        val days = listOf(DaySchedule(dayTitle = "Четверг, 10 сентября", weekId = "2", weekName = "07 сентября - 13 сентября"))
        val saved = UserSelection(facultyId = "f", courseId = "c", groupId = "g", weekId = "0")

        val resolved = Weeks.resolveStored(saved, weeks, days, requireKnown = true, today = today)
        assertEquals("2", resolved?.weekId)
        assertEquals("07 сентября - 13 сентября", resolved?.weekName)
    }

    @Test
    fun resolveStoredKeepsValidWeekAndAllOption() {
        val days = listOf(DaySchedule(dayTitle = "x", weekId = "2"))
        assertNull(Weeks.resolveStored(UserSelection(weekId = "2"), weeks, days, requireKnown = true, today = today))
        assertNull(Weeks.resolveStored(UserSelection(weekId = Weeks.ALL_ID), weeks, days, requireKnown = true, today = today))
    }

    @Test
    fun resolveStoredRequireKnownControlsUnknownWeek() {
        val days = listOf(DaySchedule(dayTitle = "Четверг, 10 сентября", weekId = "2"))
        val saved = UserSelection(weekId = "3")
        assertTrue(Weeks.resolveStored(saved, weeks, days, requireKnown = true, today = today) != null)
        assertNull(Weeks.resolveStored(saved, weeks, days, requireKnown = false, today = today))
    }

    @Test
    fun resolveAfterRefreshPicksCurrentWeekOnManualRefresh() {
        val days = listOf(
            DaySchedule(dayTitle = "Четверг, 03 сентября", weekId = "1"),
            DaySchedule(dayTitle = "Четверг, 10 сентября", weekId = "2")
        )
        val current = UserSelection(weekId = "1", weekName = "31 августа - 06 сентября")

        // сегодня 10 сентября, поэтому текущая неделя это вторая
        val picked = Weeks.resolveAfterRefresh(current, weeks, days, isManualRefresh = true, today = today)
        assertEquals("2", picked?.id)
    }

    @Test
    fun resolveAfterRefreshKeepsValidWeekOnBackgroundRefresh() {
        val days = listOf(
            DaySchedule(dayTitle = "Четверг, 03 сентября", weekId = "1"),
            DaySchedule(dayTitle = "Четверг, 10 сентября", weekId = "2")
        )
        val current = UserSelection(weekId = "1", weekName = "31 августа - 06 сентября")
        assertNull(Weeks.resolveAfterRefresh(current, weeks, days, isManualRefresh = false, today = today))
    }

    @Test
    fun resolveAfterRefreshHandlesEmptyAndAll() {
        assertNull(Weeks.resolveAfterRefresh(UserSelection(weekId = "1"), emptyList(), emptyList(), true, today))
        assertNull(Weeks.resolveAfterRefresh(UserSelection(weekId = Weeks.ALL_ID), weeks, emptyList(), true, today))
    }

    @Test
    fun resolveAfterRefreshFallsBackToFirstWeekForUnknownId() {
        val picked = Weeks.resolveAfterRefresh(UserSelection(weekId = "zzz"), weeks, emptyList(), false, today)
        assertEquals("2", picked?.id)
    }
}
