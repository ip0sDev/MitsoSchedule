package mitsoschedule.core.schedule

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mitsoschedule.core.model.OptionItem
import mitsoschedule.core.model.UserSelection
import mitsoschedule.core.network.ApiError
import mitsoschedule.core.network.ApiResult
import mitsoschedule.core.network.ServerConfig
import mitsoschedule.core.network.WebWorker
import mitsoschedule.core.storage.AesGcmSecretCipher
import mitsoschedule.core.testing.InMemoryDataStore
import mitsoschedule.core.storage.ScheduleStore
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.KeyGenerator

class ScheduleRepositoryTest {

    private val selection = UserSelection(
        facultyId = "fac", formId = "Dnevnaya", courseId = "1", groupId = "g1", weekId = "1", weekName = "w1"
    )

    private val requests = mutableListOf<String>()

    /** Подставной сервер: отвечает JSON по пути запроса, остальное 404. */
    private fun fakeServer(routes: Map<String, String>): WebWorker {
        val interceptor = Interceptor { chain ->
            val url = chain.request().url
            requests += url.encodedPath + (url.query?.let { "?$it" } ?: "")
            val body = routes[url.encodedPath]
            if (body == IO_FAILURE) throw java.io.IOException("нет сети")
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(if (body != null) 200 else 404)
                .message("fake")
                .body((body ?: "").toResponseBody("application/json".toMediaType()))
                .build()
        }
        return WebWorker("http://fake", ServerConfig.newHttpClient { addInterceptor(interceptor) })
    }

    private fun newStore(): ScheduleStore {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        return ScheduleStore(InMemoryDataStore(), "", AesGcmSecretCipher { key })
    }

    private companion object {
        const val IO_FAILURE = "<<io>>"
    }

    private val weeksJson = """[{"id":"1","name":"w1"},{"id":"2","name":"w2"}]"""
    private val scheduleJson = """[{"dayTitle":"Понедельник, 31 августа","lessons":[{"time":"08.15 — 09.35","subject":"Предмет"}]}]"""

    @Test
    fun refreshLoadsAllWeeksLabelsDaysAndCachesThem() = runBlocking {
        val store = newStore()
        val repo = ScheduleRepository(
            fakeServer(
                mapOf(
                    "/api/v1/schedule/weeks" to weeksJson,
                    "/api/v1/schedule" to scheduleJson
                )
            ),
            store
        )

        val result = repo.refresh(selection, isManualRefresh = false)

        // две недели: запрос расписания на каждую
        assertEquals(2, requests.count { it.startsWith("/api/v1/schedule?") })
        assertEquals(setOf("1", "2"), result.days.map { it.weekId }.toSet())
        assertEquals("w1", result.days.first { it.weekId == "1" }.weekName)
        assertEquals(Weeks.ALL_ID, result.weeks.first().id)
        assertNotNull(result.updateTime)
        assertEquals(result.days.size, store.getCachedSchedule().size)
    }

    @Test
    fun refreshWithoutDataDoesNotOverwriteCache() = runBlocking {
        val store = newStore()
        store.saveSchedule(
            listOf(mitsoschedule.core.model.DaySchedule(dayTitle = "старое", weekId = "1")),
            "вчера",
            1L
        )
        val repo = ScheduleRepository(fakeServer(emptyMap()), store)

        val result = repo.refresh(selection, isManualRefresh = true)

        assertTrue(result.days.isEmpty())
        assertNull(result.updateTime)
        assertEquals("старое", store.getCachedSchedule().single().dayTitle)
        assertEquals("вчера", store.lastUpdateFlow.first())
    }

    @Test
    fun loadOptionsStopsWhereSelectionStops() = runBlocking {
        val repo = ScheduleRepository(
            fakeServer(
                mapOf(
                    "/api/v1/schedule/forms" to """[{"id":"Dnevnaya","name":"Дневная"}]""",
                    "/api/v1/schedule/courses" to """[{"id":"1","name":"1 курс"}]""",
                    "/api/v1/schedule/groups" to """[{"id":"g1","name":"101"}]""",
                    "/api/v1/schedule/weeks" to weeksJson
                )
            ),
            newStore()
        )

        val onlyFaculty = repo.loadOptions(UserSelection(facultyId = "fac"))
        assertEquals(1, onlyFaculty.courses.size)
        assertTrue(onlyFaculty.groups.isEmpty())
        assertTrue(onlyFaculty.weeks.isEmpty())

        val full = repo.loadOptions(selection)
        assertEquals(listOf(OptionItem("g1", "101")), full.groups)
        assertEquals(3, full.weeks.size)

        assertTrue(repo.loadOptions(UserSelection()).courses.isEmpty())
    }

    @Test
    fun loadWeekMergesIntoExistingAndSavesOnlyWhenDataArrives() = runBlocking {
        val store = newStore()
        val existing = listOf(mitsoschedule.core.model.DaySchedule(dayTitle = "old", weekId = "1", weekName = "w1"))
        val repo = ScheduleRepository(fakeServer(mapOf("/api/v1/schedule" to scheduleJson)), store)

        val (merged, time) = (repo.loadWeek(selection, OptionItem("2", "w2"), existing) as ApiResult.Success).value!!
        assertEquals(setOf("1", "2"), merged.map { it.weekId }.toSet())
        assertEquals("w2", merged.first { it.weekId == "2" }.weekName)
        assertEquals(time, store.getLastUpdateTime())

        // 404 это сбой, а не «пустая неделя»
        val failing = ScheduleRepository(fakeServer(emptyMap()), newStore())
        val failure = failing.loadWeek(selection, OptionItem("3", "w3"), existing) as ApiResult.Failure
        assertEquals(ApiError.Kind.HTTP, failure.error.kind)

        val emptyWeek = ScheduleRepository(fakeServer(mapOf("/api/v1/schedule" to "[]")), newStore())
        assertNull((emptyWeek.loadWeek(selection, OptionItem("3", "w3"), existing) as ApiResult.Success).value)
    }

    @Test
    fun refreshReportsWhyThereIsNoData() = runBlocking {
        val offline = ScheduleRepository(
            fakeServer(mapOf("/api/v1/schedule/weeks" to IO_FAILURE, "/api/v1/schedule" to IO_FAILURE)),
            newStore()
        )
        assertEquals(ApiError.Kind.NETWORK, offline.refresh(selection, true).error?.kind)

        val serverError = ScheduleRepository(fakeServer(emptyMap()), newStore())
        val http = serverError.refresh(selection, true).error
        assertEquals(ApiError.Kind.HTTP, http?.kind)
        assertEquals(404, http?.httpCode)

        val garbage = ScheduleRepository(
            fakeServer(mapOf("/api/v1/schedule/weeks" to weeksJson, "/api/v1/schedule" to "<html>oops</html>")),
            newStore()
        )
        assertEquals(ApiError.Kind.PARSE, garbage.refresh(selection, true).error?.kind)

        // сервер ответил, но расписание пустое: это не ошибка
        val published = ScheduleRepository(
            fakeServer(mapOf("/api/v1/schedule/weeks" to weeksJson, "/api/v1/schedule" to "[]")),
            newStore()
        )
        val empty = published.refresh(selection, true)
        assertTrue(empty.days.isEmpty())
        assertNull(empty.error)
    }

    @Test
    fun partialWeekFailureStillReturnsLoadedDays() = runBlocking {
        var calls = 0
        val interceptor = Interceptor { chain ->
            val url = chain.request().url
            val body = when {
                url.encodedPath.endsWith("/weeks") -> weeksJson
                url.queryParameter("weekId") == "1" -> scheduleJson
                else -> null
            }
            calls++
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(if (body != null) 200 else 500)
                .message("fake")
                .body((body ?: "").toResponseBody("application/json".toMediaType()))
                .build()
        }
        val repo = ScheduleRepository(
            WebWorker("http://fake", ServerConfig.newHttpClient { addInterceptor(interceptor) }),
            newStore()
        )

        val result = repo.refresh(selection, false)
        assertEquals(setOf("1"), result.days.map { it.weekId }.toSet())
        assertNull(result.error)
        assertEquals(3, calls)
    }

    @Test
    fun loadOptionsReportsFailureButKeepsWhatLoaded() = runBlocking {
        val repo = ScheduleRepository(
            fakeServer(mapOf("/api/v1/schedule/forms" to """[{"id":"Dnevnaya","name":"Дневная"}]""")),
            newStore()
        )
        val options = repo.loadOptions(UserSelection(facultyId = "fac"))
        assertEquals(1, options.forms.size)
        assertTrue(options.courses.isEmpty())
        assertEquals(ApiError.Kind.HTTP, options.error?.kind)
    }
}
