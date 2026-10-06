package mitsoschedule.app

import mitsoschedule.app.ui.components.DayTimelineCategory
import mitsoschedule.app.ui.components.classifyDaySchedule
import mitsoschedule.core.model.DaySchedule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DayTimelineTest {

    private val today = LocalDate.of(2026, 9, 24)

    private fun day(title: String, subtitle: String? = null) = DaySchedule(dayTitle = title, dateSubtitle = subtitle)

    @Test
    fun classifiesByDate() {
        assertEquals(DayTimelineCategory.PAST, classifyDaySchedule(day("Понедельник, 21 сентября"), today))
        assertEquals(DayTimelineCategory.TODAY, classifyDaySchedule(day("Четверг, 24 сентября"), today))
        assertEquals(DayTimelineCategory.FUTURE, classifyDaySchedule(day("Пятница, 25 сентября"), today))
    }

    @Test
    fun subtitleHasPriorityOverTitle() {
        assertEquals(DayTimelineCategory.TODAY, classifyDaySchedule(day("Четверг", "24 сентября"), today))
    }

    @Test
    fun unparsableDateCountsAsFuture() {
        assertEquals(DayTimelineCategory.FUTURE, classifyDaySchedule(day("Расписание"), today))
        assertEquals(DayTimelineCategory.FUTURE, classifyDaySchedule(day("Четверг, 31 февраля"), today))
    }

    @Test
    fun yearBoundaryIsHandled() {
        val newYear = LocalDate.of(2027, 1, 2)
        assertEquals(DayTimelineCategory.PAST, classifyDaySchedule(day("Четверг, 31 декабря"), newYear))
        val lateDecember = LocalDate.of(2026, 12, 30)
        assertEquals(DayTimelineCategory.FUTURE, classifyDaySchedule(day("Суббота, 02 января"), lateDecember))
    }
}
