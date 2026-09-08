package mitsoschedule.app

import mitsoschedule.app.data.PreferencesManager
import mitsoschedule.app.data.StudentWebWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testScheduleParser() {
        val worker = WebWorker()
        val sampleText = "Понедельник, 31 августа Время Дисциплина и преподаватель Аудитория 08.15-9.35 Администрирование информационных систем (лек) Калинин М. А. ауд. 61 09.45-11.05 (нет занятий) 11.15-12.35 Физическая культура (практ/сем) спортзал 14.40-16.05 Белорусский язык (практ/сем) Ломака А. А. 51-52 Вторник, 01 сентября Время Дисциплина и преподаватель Аудитория 9.45-11.05 Трудовое право (лек) Доцент Ковалева Е. А. каб. 51-52"

        val parsedDays = worker.parseScheduleString(sampleText)
        assertEquals(2, parsedDays.size)

        val monday = parsedDays[0]
        assertEquals("Понедельник, 31 августа", monday.dayTitle)
        assertEquals(4, monday.lessons.size)

        // Test single digit hour format e.g. "08.15-9.35" from user's screenshot
        val lesson1 = monday.lessons[0]
        assertEquals("08.15 — 09.35", lesson1.time)
        assertEquals("Администрирование информационных систем", lesson1.subject)
        assertEquals("Лекция", lesson1.type)
        assertEquals("Калинин М. А.", lesson1.teacher)
        assertEquals("ауд. 61", lesson1.room)
        assertFalse(lesson1.isEmptyWindow)

        val lesson2 = monday.lessons[1]
        assertTrue(lesson2.isEmptyWindow)

        val lesson3 = monday.lessons[2]
        assertEquals("11.15 — 12.35", lesson3.time)
        assertEquals("Практика / Семинар", lesson3.type)
        assertEquals("спортзал", lesson3.room)

        // Test dual room format "51-52"
        val lesson4 = monday.lessons[3]
        assertEquals("14.40 — 16.05", lesson4.time)
        assertEquals("Белорусский язык", lesson4.subject)
        assertEquals("Практика / Семинар", lesson4.type)
        assertEquals("Ломака А. А.", lesson4.teacher)
        assertEquals("ауд. 51-52", lesson4.room)

        // Test Tuesday "9.45-11.05", "каб. 51-52" and "Доцент Ковалева Е. А."
        val tuesday = parsedDays[1]
        val tLesson1 = tuesday.lessons[0]
        assertEquals("09.45 — 11.05", tLesson1.time)
        assertEquals("Трудовое право", tLesson1.subject)
        assertEquals("Лекция", tLesson1.type)
        assertEquals("Доцент Ковалева Е. А.", tLesson1.teacher)
        assertEquals("каб. 51-52", tLesson1.room)
    }

    @Test
    fun testStudentCabinetParser() {
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
                <div class="notice">Важно! Данные обновляются ежедневно в 13.00, и соответствуют факту баланса лицевого счета за предыдущий день.</div>
                
                <div class="section-title">Доступ к системе дистанционного обучения на базе LMS Moodle</div>
                <div class="field">Группа: 2423</div>
                <div class="field">Логин: 419445</div>
                <div class="field">Пароль: bbb01937</div>
                <div class="notice">Уважаемые студенты! При первом входе в систему дистанционного обучения необходимо ввести и подтвердить адрес электронной почты!</div>
            </body>
            </html>
        """.trimIndent()

        val parsed = studentWorker.parseCabinetHtml(sampleHtml)
        assertEquals("Корзун Денис Алексеевич", parsed.fullName)
        assertEquals("28-08-2026", parsed.accountDate)
        assertEquals("0.00", parsed.balance)
        assertEquals("0.00", parsed.mainDebt)
        assertEquals("0.00", parsed.penalty)
        assertEquals("2423", parsed.moodleGroup)
        assertEquals("419445", parsed.moodleLogin)
        assertEquals("bbb01937", parsed.moodlePassword)
        assertFalse(parsed.isDebt)
    }

    @Test
    fun testAutoRefreshPolicy() {
        // Zero timestamp -> always refresh
        assertTrue(PreferencesManager.shouldAutoRefresh(0L))

        // Fresh cache (10 seconds ago) -> should NOT refresh
        val recentMillis = System.currentTimeMillis() - 10_000L
        assertFalse(PreferencesManager.shouldAutoRefresh(recentMillis))

        // Old cache (> 8 days) -> should refresh
        val eightDaysAgo = System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000L)
        assertTrue(PreferencesManager.shouldAutoRefresh(eightDaysAgo))
    }
}