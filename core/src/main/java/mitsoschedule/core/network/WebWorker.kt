package mitsoschedule.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mitsoschedule.core.model.DaySchedule
import mitsoschedule.core.model.OptionItem
import mitsoschedule.core.model.ServerHealth
import mitsoschedule.core.model.UserSelection
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** Клиент API расписания: эндпоинты под /api/v1/schedule и /health. */
class WebWorker(
    baseUrl: String = ServerConfig.DEFAULT_SERVER_URL,
    private val client: OkHttpClient = ServerConfig.newHttpClient()
) {

    private val TAG = "WebWorker"
    private var baseUrl: String = baseUrl.trimEnd('/')

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Недели и расписание. [error] заполнен, только если данных получить не удалось;
     * частичный успех (часть недель загрузилась) ошибкой не считается.
     */
    data class ScheduleFetchResult(
        val weeks: List<OptionItem> = emptyList(),
        val daySchedules: List<DaySchedule> = emptyList(),
        val error: ApiError? = null
    )

    fun updateBaseUrl(newUrl: String) {
        baseUrl = newUrl.trimEnd('/')
    }

    suspend fun checkHealth(): ServerHealth = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$baseUrl/health").get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ServerHealth(status = "error", service = "university", version = "unknown")
                }
                json.decodeFromString<ServerHealth>(response.body.string())
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkHealth failed: ${e.message}", e)
            ServerHealth(status = "unreachable", service = "university", version = "")
        }
    }

    suspend fun fetchFaculties(): ApiResult<List<OptionItem>> = fetchList(scheduleUrl("faculties"))

    suspend fun fetchEducationForms(facultyId: String): ApiResult<List<OptionItem>> {
        if (facultyId.isBlank()) return ApiResult.Success(emptyList())
        return fetchList(scheduleUrl("forms", "facultyId" to facultyId))
    }

    suspend fun fetchCourses(facultyId: String, formId: String): ApiResult<List<OptionItem>> {
        if (facultyId.isBlank()) return ApiResult.Success(emptyList())
        return fetchList(
            scheduleUrl("courses", "facultyId" to facultyId, "formId" to formId.ifBlank { DEFAULT_FORM_ID })
        )
    }

    suspend fun fetchGroups(facultyId: String, formId: String, courseId: String): ApiResult<List<OptionItem>> {
        if (facultyId.isBlank() || courseId.isBlank()) return ApiResult.Success(emptyList())
        return fetchList(
            scheduleUrl(
                "groups",
                "facultyId" to facultyId,
                "formId" to formId.ifBlank { DEFAULT_FORM_ID },
                "courseId" to courseId
            )
        )
    }

    suspend fun fetchWeeks(
        facultyId: String,
        formId: String,
        courseId: String,
        groupId: String
    ): ApiResult<List<OptionItem>> {
        if (facultyId.isBlank() || courseId.isBlank() || groupId.isBlank()) return ApiResult.Success(emptyList())
        return fetchList(
            scheduleUrl(
                "weeks",
                "facultyId" to facultyId,
                "formId" to formId.ifBlank { DEFAULT_FORM_ID },
                "courseId" to courseId,
                "groupId" to groupId
            )
        )
    }

    /** Список недель и расписание на все реальные недели (параллельно). */
    suspend fun fetchScheduleAndAvailableWeeks(selection: UserSelection): ScheduleFetchResult {
        val weeksResult = fetchWeeks(selection.facultyId, selection.formId, selection.courseId, selection.groupId)
        val weeks = weeksResult.orEmpty()
        val days = fetchWeeksSchedule(selection, weeks.filter(::isRealWeek))
        return ScheduleFetchResult(
            weeks = weeks,
            daySchedules = days.orEmpty(),
            error = if (days.orEmpty().isEmpty()) days.errorOrNull ?: weeksResult.errorOrNull else null
        )
    }

    /** Расписание на заданные недели; пустой [weekIds] означает «все доступные недели». */
    suspend fun fetchScheduleForWeeks(selection: UserSelection, weekIds: List<String>): ApiResult<List<DaySchedule>> {
        val weeks = if (weekIds.isNotEmpty()) {
            weekIds.map { OptionItem(id = it, name = "") }
        } else {
            val result = fetchWeeks(selection.facultyId, selection.formId, selection.courseId, selection.groupId)
            result.errorOrNull?.let { return ApiResult.Failure(it) }
            result.orEmpty()
        }
        return fetchWeeksSchedule(selection, weeks.filter(::isRealWeek))
    }

    suspend fun fetchSingleWeekSchedule(
        selection: UserSelection,
        weekId: String,
        weekName: String = ""
    ): ApiResult<List<DaySchedule>> =
        fetchWeekWithLabels(selection, OptionItem(id = weekId.ifBlank { DEFAULT_WEEK_ID }, name = weekName))

    suspend fun fetchSchedule(selection: UserSelection): ApiResult<List<DaySchedule>> =
        fetchScheduleInternal(selection, selection.weekId.ifBlank { DEFAULT_WEEK_ID })

    /**
     * Загружает недели параллельно. Если часть недель не загрузилась, а часть да, возвращает
     * то, что есть; ошибка возвращается, только если не загрузилось ничего.
     */
    private suspend fun fetchWeeksSchedule(
        selection: UserSelection,
        realWeeks: List<OptionItem>
    ): ApiResult<List<DaySchedule>> {
        if (realWeeks.isEmpty()) {
            return fetchScheduleInternal(selection, selection.weekId.ifBlank { DEFAULT_WEEK_ID })
        }
        val results = coroutineScope {
            realWeeks.map { week -> async { fetchWeekWithLabels(selection, week) } }.awaitAll()
        }
        val days = results.flatMap { it.orEmpty() }
        if (days.isEmpty()) {
            results.firstNotNullOfOrNull { it.errorOrNull }?.let { return ApiResult.Failure(it) }
        }
        return ApiResult.Success(days)
    }

    /** Подставляет id/название недели в дни, для которых сервер их не вернул. */
    private suspend fun fetchWeekWithLabels(selection: UserSelection, week: OptionItem): ApiResult<List<DaySchedule>> =
        when (val result = fetchScheduleInternal(selection, week.id)) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> ApiResult.Success(
                result.value.map { day ->
                    day.copy(
                        weekId = if (day.weekId.isBlank() || day.weekId == "0") week.id else day.weekId,
                        weekName = day.weekName.ifBlank { week.name }
                    )
                }
            )
        }

    private suspend fun fetchScheduleInternal(selection: UserSelection, weekId: String): ApiResult<List<DaySchedule>> {
        val url = scheduleUrl(
            null,
            "facultyId" to selection.facultyId,
            "formId" to selection.formId.ifBlank { DEFAULT_FORM_ID },
            "courseId" to selection.courseId,
            "groupId" to selection.groupId,
            "weekId" to weekId
        )
        return get(url) { json.decodeFromString<List<DaySchedule>>(it) }
    }

    private suspend fun fetchList(url: HttpUrl?): ApiResult<List<OptionItem>> =
        get(url) { json.decodeFromString<List<OptionItem>>(it) }

    /** GET с разбором тела; любые сбои превращаются в [ApiError], исключения наружу не выходят. */
    private suspend fun <T> get(url: HttpUrl?, parse: (String) -> T): ApiResult<T> = withContext(Dispatchers.IO) {
        if (url == null) return@withContext ApiResult.Failure(ApiError(ApiError.Kind.NETWORK, "Некорректный адрес сервера"))
        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "GET $url failed: ${response.code}")
                    return@withContext ApiResult.Failure(ApiError(ApiError.Kind.HTTP, httpCode = response.code))
                }
                ApiResult.Success(parse(response.body.string()))
            }
        } catch (e: IllegalArgumentException) {
            // SerializationException (неверный JSON) наследует IllegalArgumentException
            Log.e(TAG, "GET $url: bad response: ${e.message}", e)
            ApiResult.Failure(ApiError(ApiError.Kind.PARSE, e.message))
        } catch (e: IOException) {
            Log.e(TAG, "GET $url: ${e.message}", e)
            ApiResult.Failure(ApiError(ApiError.Kind.NETWORK, e.message))
        }
    }

    private fun scheduleUrl(path: String?, vararg params: Pair<String, String>): HttpUrl? {
        val base = if (path == null) "$baseUrl/api/v1/schedule" else "$baseUrl/api/v1/schedule/$path"
        val builder = base.toHttpUrlOrNull()?.newBuilder() ?: return null
        params.forEach { (name, value) -> builder.addQueryParameter(name, value) }
        return builder.build()
    }

    private fun isRealWeek(week: OptionItem) = week.id.isNotBlank() && week.id != ALL_WEEKS_ID

    private companion object {
        const val DEFAULT_FORM_ID = "Dnevnaya"
        const val DEFAULT_WEEK_ID = "1"
        const val ALL_WEEKS_ID = "ALL"
    }
}
