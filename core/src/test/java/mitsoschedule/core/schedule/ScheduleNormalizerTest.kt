package mitsoschedule.core.schedule

import mitsoschedule.core.model.DaySchedule
import mitsoschedule.core.model.Lesson
import mitsoschedule.core.model.SubgroupInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleNormalizerTest {

    @Test
    fun testCleanSubjectTitle() {
        val dirtyTitle = "2. Администрирование информационных систем 63 (к) / 1. Администрирование информационных систем 62 (к)"
        val cleaned = ScheduleNormalizer.cleanSubjectTitle(dirtyTitle)
        assertEquals("Администрирование информационных систем", cleaned)

        val singleWithNumber = "1. Веб-дизайн и шаблоны проектирования"
        assertEquals("Веб-дизайн и шаблоны проектирования", ScheduleNormalizer.cleanSubjectTitle(singleWithNumber))

        val withRoomAndType = "Администрирование информационных систем 63 (к) (лаб)"
        assertEquals("Администрирование информационных систем", ScheduleNormalizer.cleanSubjectTitle(withRoomAndType))
    }

    @Test
    fun testExtractRoom() {
        val (room1, rest1) = ScheduleNormalizer.extractRoomFromText("Администрирование информационных систем 63 (к)")
        assertEquals("ауд. 63 (к)", room1)
        assertEquals("Администрирование информационных систем", rest1)

        val (room2, rest2) = ScheduleNormalizer.extractRoomFromText("Ломака А. А. 51-52")
        assertEquals("ауд. 51-52", room2)
        assertEquals("Ломака А. А.", rest2)

        val (room3, rest3) = ScheduleNormalizer.extractRoomFromText("спортзал")
        assertEquals("спортзал", room3)
        assertEquals("", rest3)
    }

    @Test
    fun testNormalizeLessonDirtyCache() {
        // Simulating a lesson previously saved in DataStore with dirty merged title and null rooms in subgroups
        val dirtyCachedLesson = Lesson(
            time = "11.15 — 12.35",
            subject = "2. Администрирование информационных систем 63 (к) / 1. Администрирование информационных систем 62 (к)",
            type = "Лабораторная",
            rawText = "11.15-12.35 2. Администрирование информационных систем 63 (к) (лаб) Пархимович А. В. / 1. Администрирование информационных систем 62 (к) (лаб) Калинин М. А.",
            subgroups = listOf(
                SubgroupInfo(subgroup = "1 подгруппа", teacher = "Калинин М. А.", room = null),
                SubgroupInfo(subgroup = "2 подгруппа", teacher = "Пархимович А. В.", room = null)
            )
        )

        val normalized = ScheduleNormalizer.normalizeLesson(dirtyCachedLesson)
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
    fun testLessonsWithSameTimeAreMergedIntoSubgroups() {
        val lessons = listOf(
            Lesson(
                time = "11.15 — 12.35",
                subject = "Администрирование информационных систем",
                type = "Лабораторная",
                teacher = "Пархимович А. В.",
                room = "ауд. 63 (к)",
                subgroup = "2 подгруппа"
            ),
            Lesson(
                time = "11.15 — 12.35",
                subject = "Администрирование информационных систем",
                type = "Лабораторная",
                teacher = "Калинин М. А.",
                room = "ауд. 62 (к)",
                subgroup = "1 подгруппа"
            ),
            Lesson(time = "13.05 — 14.25", subject = "Философия", type = "Лекция", room = "ауд. 100")
        )

        val result = ScheduleNormalizer.groupLessonsByTime(lessons.map { ScheduleNormalizer.normalizeLesson(it) })
        assertEquals(2, result.size)

        val lab = result[0]
        assertEquals("11.15 — 12.35", lab.time)
        assertEquals("Администрирование информационных систем", lab.subject)
        assertEquals("Лабораторная", lab.type)
        assertEquals(2, lab.subgroups.size)
        // Первая подгруппа идёт первой из-за сортировки
        assertEquals("1 подгруппа", lab.subgroups[0].subgroup)
        assertEquals("Калинин М. А.", lab.subgroups[0].teacher)
        assertEquals("ауд. 62 (к)", lab.subgroups[0].room)
        assertEquals("2 подгруппа", lab.subgroups[1].subgroup)
        assertEquals("Пархимович А. В.", lab.subgroups[1].teacher)

        val regular = result[1]
        assertEquals("Философия", regular.subject)
        assertEquals("ауд. 100", regular.room)
        assertTrue(regular.subgroups.isEmpty())
    }

    @Test
    fun testSingleSubgroupLessonIsWrappedIntoSubgroupList() {
        val lesson = Lesson(
            time = "13.05 — 14.25",
            subject = "Веб-дизайн и шаблоны проектирования",
            teacher = "Пархимович А. В.",
            room = "ауд. 63 (к)",
            subgroup = "1 подгруппа"
        )

        val result = ScheduleNormalizer.groupLessonsByTime(listOf(ScheduleNormalizer.normalizeLesson(lesson)))
        assertEquals(1, result.size)
        assertEquals(1, result[0].subgroups.size)
        assertEquals("1 подгруппа", result[0].subgroups[0].subgroup)
        assertEquals("ауд. 63 (к)", result[0].subgroups[0].room)
    }

    @Test
    fun testEmptyWindowsAreKeptAsIs() {
        val window = Lesson(time = "09.45 — 11.05", subject = "Нет занятий", isEmptyWindow = true)
        assertEquals(listOf(window), ScheduleNormalizer.groupLessonsByTime(listOf(window)))
    }
}
