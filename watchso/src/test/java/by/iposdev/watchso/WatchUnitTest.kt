package by.iposdev.watchso

import by.iposdev.watchso.data.StudentWebWorker
import by.iposdev.watchso.data.WebWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchUnitTest {
    @Test
    fun testWatchScheduleParser() {
        val worker = WebWorker()
        val sampleText = "Понедельник, 31 августа Время Дисциплина и преподаватель Аудитория 08.15-9.35 Администрирование информационных систем (лек) Калинин М. А. ауд. 61 09.45-11.05 (нет занятий) 14.40-16.05 Белорусский язык (практ/сем) Ломака А. А. 51-52 Вторник, 01 сентября Время Дисциплина и преподаватель Аудитория 9.45-11.05 Трудовое право (лек) Доцент Ковалева Е. А. каб. 51-52"

        val parsedDays = worker.parseScheduleString(sampleText)
        assertEquals(2, parsedDays.size)

        val monday = parsedDays[0]
        assertEquals("Понедельник, 31 августа", monday.dayTitle)
        assertEquals(3, monday.lessons.size)

        // Single digit hour & normalized format
        val lesson1 = monday.lessons[0]
        assertEquals("08.15 — 09.35", lesson1.time)
        assertEquals("Администрирование информационных систем", lesson1.subject)
        assertEquals("Лекция", lesson1.type)
        assertEquals("Калинин М. А.", lesson1.teacher)
        assertEquals("ауд. 61", lesson1.room)

        // Dual room 51-52
        val lesson3 = monday.lessons[2]
        assertEquals("14.40 — 16.05", lesson3.time)
        assertEquals("ауд. 51-52", lesson3.room)
        assertEquals("Ломака А. А.", lesson3.teacher)
    }

    @Test
    fun testWatchStudentCabinetParser() {
        val studentWorker = StudentWebWorker()
        val sampleHtml = """
            <html>
            <body>
                <div class="header">СТУДЕНТ</div>
                <div class="student-name">Корзун Денис Алексеевич</div>
                <div class="section-title">Состояние лицевого счета на конец дня 28-08-2026:</div>
                <div class="field">Баланс: 0.00</div>
                <div class="field">Основной долг: 0.00</div>
                <div class="field">Пеня за просрочку платежа + процент за пользование чужими денежными средствами: 0.00</div>
                
                <div class="section-title">Доступ к системе дистанционного обучения на базе LMS Moodle</div>
                <div class="field">Группа: 2423</div>
                <div class="field">Логин: 419445</div>
                <div class="field">Пароль: bbb01937</div>
            </body>
            </html>
        """.trimIndent()

        val parsed = studentWorker.parseCabinetHtml(sampleHtml)
        assertEquals("Корзун Денис Алексеевич", parsed.fullName)
        assertEquals("Корзун Д. А.", parsed.initialsName)
        assertEquals("28-08-2026", parsed.accountDate)
        assertEquals("0.00", parsed.balance)
        assertEquals("2423", parsed.moodleGroup)
        assertEquals("419445", parsed.moodleLogin)
        assertEquals("bbb01937", parsed.moodlePassword)
        assertFalse(parsed.isDebt)
    }

    @Test
    fun testDepDropJsonNumericIdParsing() {
        val worker = WebWorker()
        val sampleWeeksJson = """{"output":[{"id":1,"name":"31 августа - 06 сентября"}],"selected":""}"""
        val parsed = worker.parseDepDropJson(sampleWeeksJson)
        assertEquals(1, parsed.size)
        assertEquals("1", parsed[0].id)
        assertEquals("31 августа - 06 сентября", parsed[0].name)

        val sampleStringIdJson = """{"output":[{"id":"2631 PR","name":"2631 ПР"}],"selected":""}"""
        val parsedGroups = worker.parseDepDropJson(sampleStringIdJson)
        assertEquals(1, parsedGroups.size)
        assertEquals("2631 PR", parsedGroups[0].id)
        assertEquals("2631 ПР", parsedGroups[0].name)
    }

    @Test
    fun testSkipDayWithOnlyEmptyWindows() {
        val worker = WebWorker()
        val emptyMondayText = "Понедельник, 31 августа Время Дисциплина и преподаватель Аудитория 08.15-9.35 (нет занятий) 09.45-11.05 (нет занятий) 11.15-12.35 (нет занятий) Вторник, 01 сентября Время Дисциплина и преподаватель Аудитория 08.15-9.35 Гражданское право (лек) Доцент Иванов И. И. ауд. 312"

        val parsedDays = worker.parseScheduleString(emptyMondayText)
        assertEquals(1, parsedDays.size)
        assertEquals("Вторник, 01 сентября", parsedDays[0].dayTitle)
        assertEquals(1, parsedDays[0].lessons.size)
        assertEquals("Гражданское право", parsedDays[0].lessons[0].subject)
    }

    @Test
    fun testAutoRefreshPolicy() {
        // Zero timestamp -> always refresh
        assertTrue(by.iposdev.watchso.data.WatchPreferencesManager.shouldAutoRefresh(0L))

        // Fresh cache (10 seconds ago) -> should NOT refresh
        val recentMillis = System.currentTimeMillis() - 10_000L
        assertFalse(by.iposdev.watchso.data.WatchPreferencesManager.shouldAutoRefresh(recentMillis))

        // Old cache (> 8 days) -> should refresh
        val eightDaysAgo = System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000L)
        assertTrue(by.iposdev.watchso.data.WatchPreferencesManager.shouldAutoRefresh(eightDaysAgo))
    }
}
