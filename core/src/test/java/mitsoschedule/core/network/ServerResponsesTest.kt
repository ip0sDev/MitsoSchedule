package mitsoschedule.core.network

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mitsoschedule.core.model.DaySchedule
import mitsoschedule.core.model.Lesson
import mitsoschedule.core.model.UserSelection
import mitsoschedule.core.testing.FakeServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Клиент должен переживать изменения формата ответа сервера: новые поля, null, неполные объекты. */
class ServerResponsesTest {

    private fun worker(path: String, body: String) =
        WebWorker("http://fake", FakeServer(mapOf(path to body)).client())

    private val selection = UserSelection(facultyId = "f", courseId = "c", groupId = "g", weekId = "1")

    @Test
    fun unknownFieldsAndNullsAreTolerated() = runBlocking {
        val body = """
            [{
              "dayTitle": "Понедельник, 31 августа",
              "dateSubtitle": null,
              "weekId": null,
              "newServerField": {"nested": [1, 2, 3]},
              "lessons": [{
                "time": "08.15 — 09.35",
                "subject": "Предмет",
                "teacher": null,
                "room": "ауд. 312",
                "futureFlag": true,
                "subgroups": [{"subgroup": "1 подгруппа", "teacher": null, "extra": 1}]
              }]
            }]
        """.trimIndent()

        val result = worker("/api/v1/schedule", body).fetchSchedule(selection)

        val day = (result as ApiResult.Success).value.single()
        assertNull(day.dateSubtitle)
        assertEquals("0", day.weekId)
        val lesson = day.lessons.single()
        assertNull(lesson.teacher)
        assertEquals("ауд. 312", lesson.room)
        assertEquals("1 подгруппа", lesson.subgroups.single().subgroup)
    }

    @Test
    fun missingRequiredFieldIsReportedAsParseError() = runBlocking {
        // у DaySchedule нет значения по умолчанию для dayTitle
        val result = worker("/api/v1/schedule", """[{"lessons": []}]""").fetchSchedule(selection)
        assertEquals(ApiError.Kind.PARSE, result.errorOrNull?.kind)
    }

    @Test
    fun optionListsIgnoreExtraFields() = runBlocking {
        val body = """[{"id":"2631 PR","name":"2631 ПР","order":1},{"id":"2","name":"2 курс"}]"""
        val result = worker("/api/v1/schedule/groups", body).fetchGroups("f", "Dnevnaya", "c")
        assertEquals(listOf("2631 PR", "2"), (result as ApiResult.Success).value.map { it.id })
    }

    @Test
    fun blankArgumentsDoNotHitTheNetwork() = runBlocking {
        val server = FakeServer()
        val api = WebWorker("http://fake", server.client())

        assertTrue(api.fetchCourses("", "Dnevnaya").orEmpty().isEmpty())
        assertTrue(api.fetchGroups("f", "Dnevnaya", "").orEmpty().isEmpty())
        assertTrue(api.fetchWeeks("f", "Dnevnaya", "c", "").orEmpty().isEmpty())
        assertTrue(server.requests.isEmpty())
    }

    @Test
    fun cachedModelsRoundTripWithDefaults() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val day = DaySchedule(dayTitle = "Вторник", lessons = listOf(Lesson(subject = "Предмет", isEmptyWindow = false)))
        assertEquals(day, json.decodeFromString<DaySchedule>(json.encodeToString(day)))

        // кэш, записанный старой версией без новых полей, читается с значениями по умолчанию
        val old = json.decodeFromString<UserSelection>("""{"facultyId":"f","courseId":"c","groupId":"g"}""")
        assertEquals("Dnevnaya", old.formId)
        assertEquals("0", old.weekId)
        assertTrue(old.isComplete)
    }

    @Test
    fun serverUrlTrailingSlashIsIgnored() = runBlocking {
        val server = FakeServer(mapOf("/api/v1/schedule/faculties" to "[]"))
        val api = WebWorker("http://fake/", server.client())
        api.fetchFaculties()
        api.updateBaseUrl("http://other///")
        api.fetchFaculties()
        assertEquals(listOf("/api/v1/schedule/faculties", "/api/v1/schedule/faculties"), server.requests.toList())
    }
}
