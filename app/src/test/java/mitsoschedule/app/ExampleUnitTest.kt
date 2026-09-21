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

    @Test
    fun testSubgroupParsingAndGrouping() {
        val worker = WebWorker()
        val text = "Понедельник, 31 августа Время Дисциплина и преподаватель Аудитория 08.15-09.35 Иностранный язык (практ) 1 п/г Иванов И. И. ауд. 312 08.15-09.35 Иностранный язык (практ) 2 п/г Петров П. П. ауд. 314 09.45-11.05 Философия (лек) Доцент Сидоров С. С. ауд. 100"

        val parsedDays = worker.parseScheduleString(text)
        assertEquals(1, parsedDays.size)

        val monday = parsedDays[0]
        // The two 08.15-09.35 lessons must be merged into a single card
        assertEquals(2, monday.lessons.size)

        val merged = monday.lessons[0]
        assertEquals("08.15 — 09.35", merged.time)
        assertEquals("Иностранный язык", merged.subject)
        assertEquals(2, merged.subgroups.size)

        val sg1 = merged.subgroups[0]
        assertEquals("1 подгруппа", sg1.subgroup)
        assertEquals("Иванов И. И.", sg1.teacher)
        assertEquals("ауд. 312", sg1.room)

        val sg2 = merged.subgroups[1]
        assertEquals("2 подгруппа", sg2.subgroup)
        assertEquals("Петров П. П.", sg2.teacher)
        assertEquals("ауд. 314", sg2.room)

        val regular = monday.lessons[1]
        assertEquals("09.45 — 11.05", regular.time)
        assertEquals("Философия", regular.subject)
        assertEquals("Доцент Сидоров С. С.", regular.teacher)
        assertEquals("ауд. 100", regular.room)
        assertTrue(regular.subgroups.isEmpty())
    }

    @Test
    fun testTodayTimeInfoCalculation() {
        val lessons = listOf(
            mitsoschedule.app.data.Lesson(time = "08.15 — 09.35", subject = "Пара 1"),
            mitsoschedule.app.data.Lesson(time = "09.45 — 11.05", subject = "Пара 2"),
            mitsoschedule.app.data.Lesson(time = "14.40 — 16.05", subject = "Пара 3")
        )

        // 1. Morning before classes (07:30)
        val morningInfo = mitsoschedule.app.ui.components.calculateTodayTimeInfo(
            lessons,
            java.time.LocalTime.of(7, 30)
        )
        assertEquals(mitsoschedule.app.ui.components.TodayScheduleState.NOT_STARTED, morningInfo.state)
        assertEquals(0, morningInfo.nextLessonIndex)
        assertEquals("Пары ещё не начались", morningInfo.infoMessage)

        // 2. During first class (08:30)
        val duringClassInfo = mitsoschedule.app.ui.components.calculateTodayTimeInfo(
            lessons,
            java.time.LocalTime.of(8, 30)
        )
        assertEquals(mitsoschedule.app.ui.components.TodayScheduleState.ONGOING_LESSON, duringClassInfo.state)
        assertEquals(0, duringClassInfo.currentLessonIndex)

        // 3. During break between pair 1 and 2 (09:40)
        val breakInfo = mitsoschedule.app.ui.components.calculateTodayTimeInfo(
            lessons,
            java.time.LocalTime.of(9, 40)
        )
        assertEquals(mitsoschedule.app.ui.components.TodayScheduleState.BREAK_BETWEEN_LESSONS, breakInfo.state)
        assertEquals(1, breakInfo.nextLessonIndex)

        // 4. Evening after all classes (17:00)
        val eveningInfo = mitsoschedule.app.ui.components.calculateTodayTimeInfo(
            lessons,
            java.time.LocalTime.of(17, 0)
        )
        assertEquals(mitsoschedule.app.ui.components.TodayScheduleState.FINISHED, eveningInfo.state)
        assertEquals("Пары на сегодня закончились", eveningInfo.infoMessage)
    }

    @Test
    fun testScreenshotLabSubgroupCase() {
        val worker = WebWorker()
        val text = "Понедельник, 21 сентября Время Дисциплина и преподаватель Аудитория 11.15-12.35 2. Администрирование информационных систем (лаб) Пархимович А. В. 63 (к) 11.15-12.35 1. Администрирование информационных систем (лаб) Калинин М. А. 62 (к)"

        val parsedDays = worker.parseScheduleString(text)
        assertEquals(1, parsedDays.size)

        val monday = parsedDays[0]
        assertEquals(1, monday.lessons.size)

        val lab = monday.lessons[0]
        assertEquals("11.15 — 12.35", lab.time)
        assertEquals("Администрирование информационных систем", lab.subject)
        assertEquals("Лабораторная", lab.type)
        assertEquals(2, lab.subgroups.size)

        // Subgroup 1 must come first because of sorting
        val sg1 = lab.subgroups[0]
        assertEquals("1 подгруппа", sg1.subgroup)
        assertEquals("Калинин М. А.", sg1.teacher)
        assertEquals("ауд. 62 (к)", sg1.room)

        // Subgroup 2
        val sg2 = lab.subgroups[1]
        assertEquals("2 подгруппа", sg2.subgroup)
        assertEquals("Пархимович А. В.", sg2.teacher)
        assertEquals("ауд. 63 (к)", sg2.room)
    }

    @Test
    fun testCleanSubjectTitle() {
        val dirtyTitle = "2. Администрирование информационных систем 63 (к) / 1. Администрирование информационных систем 62 (к)"
        val cleaned = WebWorker.cleanSubjectTitle(dirtyTitle)
        assertEquals("Администрирование информационных систем", cleaned)

        val singleWithNumber = "1. Веб-дизайн и шаблоны проектирования"
        assertEquals("Веб-дизайн и шаблоны проектирования", WebWorker.cleanSubjectTitle(singleWithNumber))

        val withRoomAndType = "Администрирование информационных систем 63 (к) (лаб)"
        assertEquals("Администрирование информационных систем", WebWorker.cleanSubjectTitle(withRoomAndType))
    }

    @Test
    fun testExtractRoom() {
        val (room1, rest1) = WebWorker.extractRoomFromText("Администрирование информационных систем 63 (к)")
        assertEquals("ауд. 63 (к)", room1)
        assertEquals("Администрирование информационных систем", rest1)

        val (room2, rest2) = WebWorker.extractRoomFromText("Ломака А. А. 51-52")
        assertEquals("ауд. 51-52", room2)
        assertEquals("Ломака А. А.", rest2)

        val (room3, rest3) = WebWorker.extractRoomFromText("спортзал")
        assertEquals("спортзал", room3)
        assertEquals("", rest3)
    }

    @Test
    fun testNormalizeLessonDirtyCache() {
        // Simulating a lesson previously saved in DataStore with dirty merged title and null rooms in subgroups
        val dirtyCachedLesson = mitsoschedule.app.data.Lesson(
            time = "11.15 — 12.35",
            subject = "2. Администрирование информационных систем 63 (к) / 1. Администрирование информационных систем 62 (к)",
            type = "Лабораторная",
            rawText = "11.15-12.35 2. Администрирование информационных систем 63 (к) (лаб) Пархимович А. В. / 1. Администрирование информационных систем 62 (к) (лаб) Калинин М. А.",
            subgroups = listOf(
                mitsoschedule.app.data.SubgroupInfo(subgroup = "1 подгруппа", teacher = "Калинин М. А.", room = null),
                mitsoschedule.app.data.SubgroupInfo(subgroup = "2 подгруппа", teacher = "Пархимович А. В.", room = null)
            )
        )

        val normalized = WebWorker.normalizeLesson(dirtyCachedLesson)
        assertEquals("Администрирование информационных систем", normalized.subject)
        assertEquals(2, normalized.subgroups.size)

        val sg1 = normalized.subgroups[0]
        assertEquals("1 подгруппа", sg1.subgroup)
        assertEquals("Калинин М. А.", sg1.teacher)
        assertEquals("ауд. 62 (к)", sg1.room)

        val sg2 = normalized.subgroups[1]
        assertEquals("2 подгруппа", sg2.subgroup)
        assertEquals("Пархимович А. В.", sg2.teacher)
        assertEquals("ауд. 63 (к)", sg2.room)
    }

    @Test
    fun testScreenshotFullDayScenario() {
        val worker = WebWorker()
        val text = "Понедельник, 21 сентября Время Дисциплина и преподаватель Аудитория 11.15-12.35 2. Администрирование информационных систем 63 (к) (лаб) Пархимович А. В. 11.15-12.35 1. Администрирование информационных систем 62 (к) (лаб) Калинин М. А. 13.05-14.25 1. Веб-дизайн и шаблоны проектирования 63 (к) (лаб) Пархимович А. В."

        val parsed = worker.parseScheduleString(text)
        assertEquals(1, parsed.size)
        val lessons = parsed[0].lessons
        assertEquals(2, lessons.size)

        // Lesson 1: Merged lab
        val lab = lessons[0]
        assertEquals("11.15 — 12.35", lab.time)
        assertEquals("Администрирование информационных систем", lab.subject)
        assertEquals(2, lab.subgroups.size)
        assertEquals("1 подгруппа", lab.subgroups[0].subgroup)
        assertEquals("ауд. 62 (к)", lab.subgroups[0].room)
        assertEquals("Калинин М. А.", lab.subgroups[0].teacher)
        assertEquals("2 подгруппа", lab.subgroups[1].subgroup)
        assertEquals("ауд. 63 (к)", lab.subgroups[1].room)
        assertEquals("Пархимович А. В.", lab.subgroups[1].teacher)

        // Lesson 2: Web design with single subgroup
        val web = lessons[1]
        assertEquals("13.05 — 14.25", web.time)
        assertEquals("Веб-дизайн и шаблоны проектирования", web.subject)
        assertEquals(1, web.subgroups.size)
        assertEquals("1 подгруппа", web.subgroups[0].subgroup)
        assertEquals("ауд. 63 (к)", web.subgroups[0].room)
        assertEquals("Пархимович А. В.", web.subgroups[0].teacher)
    }
}