package mitsoschedule.app

import android.util.Log
import mitsoschedule.app.data.DaySchedule
import mitsoschedule.app.data.DepDropResponse
import mitsoschedule.app.data.Lesson
import mitsoschedule.app.data.OptionItem
import mitsoschedule.app.data.UserSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.net.CookieManager
import java.net.CookiePolicy
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class WebWorker {

    private val client: OkHttpClient
    private val TAG = "WebWorker"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private val baseUrl = "https://apps.mitso.by/frontend/web/schedule"
    private val groupScheduleUrl = "$baseUrl/group-schedule"

    init {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        val cookieManager = CookieManager()
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        val cookieJar = JavaNetCookieJar(cookieManager)

        client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    private var cachedCsrfToken: String = ""

    suspend fun getCSRFTokenAndFaculties(): Pair<String, List<OptionItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(groupScheduleUrl).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "getCSRFTokenAndFaculties failed: ${response.code}")
                        return@withContext Pair("", emptyList())
                    }
                    val html = response.body.string()
                    val doc = Jsoup.parse(html)
                    val token = doc.select("meta[name=csrf-token]").attr("content")
                    cachedCsrfToken = token

                    val faculties = doc.select("select#faculty-id option")
                        .map { OptionItem(id = it.attr("value"), name = it.text().trim()) }
                        .filter { it.id.isNotBlank() }

                    Pair(token, faculties)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching faculties: ${e.message}", e)
                Pair("", emptyList())
            }
        }
    }

    private suspend fun ensureCsrfToken(): String {
        if (cachedCsrfToken.isNotBlank()) return cachedCsrfToken
        val (token, _) = getCSRFTokenAndFaculties()
        return token
    }

    private suspend fun postDepDrop(url: String, parents: List<String>): List<OptionItem> {
        return withContext(Dispatchers.IO) {
            try {
                val token = ensureCsrfToken()
                val formBuilder = FormBody.Builder()
                    .add("_csrf-frontend", token)

                parents.forEach { parent ->
                    formBuilder.add("depdrop_parents[]", parent)
                }

                val request = Request.Builder()
                    .url(url)
                    .post(formBuilder.build())
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "DepDrop request to $url failed: ${response.code}")
                        return@withContext emptyList()
                    }
                    val responseBody = response.body.string()
                    parseDepDropJson(responseBody)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in postDepDrop for $url: ${e.message}", e)
                emptyList()
            }
        }
    }

    fun parseDepDropJson(jsonString: String): List<OptionItem> {
        return try {
            val jsonElement = json.parseToJsonElement(jsonString)
            val outputArray = jsonElement.jsonObject["output"]?.jsonArray
            outputArray?.mapNotNull { itemElement ->
                val obj = itemElement.jsonObject
                val id = when (val idPrim = obj["id"]?.jsonPrimitive) {
                    null -> ""
                    else -> idPrim.content
                }
                val name = obj["name"]?.jsonPrimitive?.content ?: ""
                if (id.isNotBlank() || name.isNotBlank()) OptionItem(id = id, name = name) else null
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing depdrop JSON: $jsonString", e)
            emptyList()
        }
    }

    suspend fun fetchEducationForms(facultyId: String): List<OptionItem> {
        return postDepDrop("$baseUrl/education", listOf(facultyId))
    }

    suspend fun fetchCourses(facultyId: String, formId: String): List<OptionItem> {
        return postDepDrop("$baseUrl/course", listOf(facultyId, formId))
    }

    suspend fun fetchGroups(facultyId: String, formId: String, courseId: String): List<OptionItem> {
        return postDepDrop("$baseUrl/group", listOf(facultyId, formId, courseId))
    }

    suspend fun fetchWeeks(facultyId: String, formId: String, courseId: String, groupId: String): List<OptionItem> {
        return postDepDrop("$baseUrl/week", listOf(facultyId, formId, courseId, groupId))
    }

    data class ScheduleFetchResult(
        val weeks: List<OptionItem> = emptyList(),
        val daySchedules: List<DaySchedule> = emptyList()
    )

    suspend fun fetchScheduleAndAvailableWeeks(selection: UserSelection): ScheduleFetchResult {
        return withContext(Dispatchers.IO) {
            val token = ensureCsrfToken()
            if (token.isEmpty()) {
                Log.e(TAG, "fetchScheduleAndAvailableWeeks - CSRF token is empty")
                return@withContext ScheduleFetchResult()
            }

            val initialWeekId = selection.weekId.ifBlank { "0" }
            val formBody = FormBody.Builder()
                .add("_csrf-frontend", token)
                .add("ScheduleSearch[fak]", selection.facultyId)
                .add("ScheduleSearch[form]", selection.formId.ifBlank { "Dnevnaya" })
                .add("ScheduleSearch[kurse]", selection.courseId)
                .add("ScheduleSearch[group_class]", selection.groupId)
                .add("ScheduleSearch[week]", if (initialWeekId == "ALL") "0" else initialWeekId)
                .build()

            val request = Request.Builder().url(groupScheduleUrl).post(formBody).build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "fetchScheduleAndAvailableWeeks - Unsuccessful response: ${response.code}")
                        return@withContext ScheduleFetchResult()
                    }
                    val html = response.body.string()
                    val doc = Jsoup.parse(html)

                    // 1. Extract week options from <select id="week-selector">
                    val extractedWeeks = doc.select("select#week-selector option, select#week-id option, select[name='ScheduleSearch[week]'] option")
                        .map { OptionItem(id = it.attr("value"), name = it.text().trim()) }
                        .filter { it.id.isNotBlank() }

                    val allDays = mutableListOf<DaySchedule>()

                    // 2. Parse ALL <div class="weekly-schedule"> elements returned in HTML
                    val scheduleDivs = doc.select("div.weekly-schedule")
                    scheduleDivs.forEachIndexed { index, div ->
                        val divId = div.attr("id").removePrefix("schedule-").trim()
                        val weekItem = extractedWeeks.find { it.id == divId || it.name == divId }
                            ?: extractedWeeks.getOrNull(index)

                        val weekId = weekItem?.id ?: divId.ifBlank { "0" }
                        val weekName = weekItem?.name ?: divId.ifBlank { "Текущая неделя" }

                        val rawText = div.text().trim()
                        val parsedDays = parseScheduleString(rawText).map { day ->
                            day.copy(weekId = weekId, weekName = weekName)
                        }
                        allDays.addAll(parsedDays)
                    }

                    // Fallback if no weekly-schedule divs found
                    if (allDays.isEmpty()) {
                        val fallbackDiv = doc.select("div.weekly-schedule").first()?.text()?.trim().orEmpty()
                        val fallbackDays = parseScheduleString(fallbackDiv)
                        allDays.addAll(fallbackDays)
                    }

                    Log.d(TAG, "SUCCESS: Extracted ${extractedWeeks.size} weeks and ${allDays.size} total days")
                    ScheduleFetchResult(weeks = extractedWeeks, daySchedules = allDays)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching schedule and weeks: ${e.message}", e)
                ScheduleFetchResult()
            }
        }
    }

    private fun logLargeString(tag: String, prefix: String, str: String) {
        val maxChunkSize = 3000
        var i = 0
        val total = str.length
        while (i < total) {
            val end = Math.min(i + maxChunkSize, total)
            Log.d(tag, "$prefix [${i / maxChunkSize + 1}]: ${str.substring(i, end)}")
            i += maxChunkSize
        }
    }

    suspend fun fetchSchedule(selection: UserSelection): List<DaySchedule> {
        return withContext(Dispatchers.IO) {
            val token = ensureCsrfToken()
            if (token.isEmpty()) {
                Log.e(TAG, "fetchSchedule - CSRF token is empty")
                return@withContext emptyList()
            }

            val formBody = FormBody.Builder()
                .add("_csrf-frontend", token)
                .add("ScheduleSearch[fak]", selection.facultyId)
                .add("ScheduleSearch[form]", selection.formId.ifBlank { "Dnevnaya" })
                .add("ScheduleSearch[kurse]", selection.courseId)
                .add("ScheduleSearch[group_class]", selection.groupId)
                .add("ScheduleSearch[week]", selection.weekId.ifBlank { "0" })
                .build()

            val request = Request.Builder().url(groupScheduleUrl).post(formBody).build()
            Log.d(TAG, "RAW_LOG: fetchSchedule POST for week='${selection.weekId}'")
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "fetchSchedule - Unsuccessful response: ${response.code}")
                        return@withContext emptyList()
                    }
                    val html = response.body.string()
                    Log.d(TAG, "RAW_LOG: fetchSchedule response html len=${html.length} for week='${selection.weekId}'")
                    logLargeString(TAG, "RAW_LOG_SINGLE_WEEK_${selection.weekId}", html)

                    val doc = Jsoup.parse(html)
                    val scheduleDiv = doc.select("div.weekly-schedule").first()
                    val rawScheduleText = scheduleDiv?.text()?.trim()

                    if (rawScheduleText.isNullOrEmpty()) {
                        Log.w(TAG, "Weekly schedule container not found in response HTML for week '${selection.weekId}'")
                        return@withContext emptyList()
                    }

                    val parsed = parseScheduleString(rawScheduleText)
                    Log.d(TAG, "RAW_LOG: fetchSchedule parsed ${parsed.size} days for week '${selection.weekId}'")
                    parsed
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching schedule: ${e.message}", e)
                emptyList()
            }
        }
    }

    fun parseScheduleString(scheduleText: String): List<DaySchedule> {
        val daySchedules = mutableListOf<DaySchedule>()
        if (scheduleText.isBlank()) return emptyList()

        val dayHeaderRegex = """(Понедельник|Вторник|Среда|Четверг|Пятница|Суббота|Воскресенье),\s*(\d{1,2}\s+\p{L}+)""".toRegex()
        val dayMatches = dayHeaderRegex.findAll(scheduleText).toList()

        if (dayMatches.isEmpty()) {
            return emptyList()
        }

        dayMatches.forEachIndexed { index, matchResult ->
            val dayTitle = matchResult.groupValues[1]
            val dateSubtitle = matchResult.groupValues[2]

            val dayBlockStartIndex = matchResult.range.last + 1
            val dayBlockEndIndex = if (index < dayMatches.size - 1) {
                dayMatches[index + 1].range.first
            } else {
                scheduleText.length
            }

            var dayContent = scheduleText.substring(dayBlockStartIndex, dayBlockEndIndex).trim()
            val lessonsBlockMarker = "Время Дисциплина и преподаватель Аудитория"
            if (dayContent.startsWith(lessonsBlockMarker)) {
                dayContent = dayContent.substring(lessonsBlockMarker.length).trim()
            }

            val lessons = mutableListOf<Lesson>()
            if (dayContent.isNotBlank()) {
                val lessonParts = dayContent.split(Regex("""(?=\b\d{1,2}[.:]\d{2}\s*[-–—]\s*\d{1,2}[.:]\d{2})"""))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                lessonParts.forEach { part ->
                    val parsed = parseSingleLesson(part)
                    lessons.add(parsed)
                }
            }

            val cleanedLessons = cleanDayLessons(lessons)
            if (cleanedLessons.isNotEmpty()) {
                daySchedules.add(DaySchedule(dayTitle = "$dayTitle, $dateSubtitle", dateSubtitle = dateSubtitle, lessons = cleanedLessons))
            }
        }

        return daySchedules
    }

    fun cleanDayLessons(lessons: List<Lesson>): List<Lesson> {
        val firstRealIndex = lessons.indexOfFirst { !it.isEmptyWindow }
        val lastRealIndex = lessons.indexOfLast { !it.isEmptyWindow }

        if (firstRealIndex == -1 || lastRealIndex == -1) {
            return emptyList() // No real classes on this day
        }

        return lessons.subList(firstRealIndex, lastRealIndex + 1)
    }

    private fun parseSingleLesson(raw: String): Lesson {
        val timeRegex = Regex("""^(\d{1,2}[.:]\d{2}\s*[-–—]\s*\d{1,2}[.:]\d{2})""")
        var timeMatch = timeRegex.find(raw)
        if (timeMatch == null) {
            timeMatch = Regex("""\b(\d{1,2}[.:]\d{2}\s*[-–—]\s*\d{1,2}[.:]\d{2})\b""").find(raw)
        }

        var text = if (timeMatch != null) {
            raw.removeRange(timeMatch.range).trim()
        } else {
            raw.trim()
        }

        val rawTime = timeMatch?.value
        val formattedTime = rawTime?.let { formatTimeRange(it) }

        if (text.contains("(нет занятий)", ignoreCase = true)) {
            return Lesson(
                time = formattedTime ?: rawTime,
                subject = "Нет занятий",
                rawText = raw,
                isEmptyWindow = true
            )
        }

        // 1. Room extraction (supports 51-52, каб. 51-52, ауд. 312, 31, спортзал, etc.)
        val roomRegexes = listOf(
            Regex("""(?:ауд\.|каб\.|аудитория|кабинет|зал|с/з|спортзал)\s*([0-9А-Яа-я/-]+(?:\s*-\s*[0-9А-Яа-я/-]+)?)""", RegexOption.IGNORE_CASE),
            Regex("""\b(\d{1,3}(?:[а-яА-Я]|-\d{1,3}|/\d{1,2})?)\s*(?:ауд|каб|аудитория)?$"""),
            Regex("""\b(спортзал|с/з|актовый зал)\b""", RegexOption.IGNORE_CASE)
        )

        var room: String? = null
        for (regex in roomRegexes) {
            val match = regex.find(text)
            if (match != null) {
                val extracted = match.value.trim()
                room = if (extracted.matches(Regex("""^\d{1,3}(?:-\d{1,3}|/\d{1,2}|[а-яА-Я])?$"""))) {
                    "ауд. $extracted"
                } else {
                    extracted
                }
                text = text.substring(0, match.range.first) + " " + text.substring(match.range.last + 1)
                text = text.trim()
                break
            }
        }

        // 2. Type extraction (лек, практ, сем, лаб, зачет, экзамен)
        var type: String? = null
        val typeRegex = Regex("""\((лек|практ|сем|лаб|консультация|зачет|экзамен|практ/сем|сем/практ)\)""", RegexOption.IGNORE_CASE)
        val typeMatch = typeRegex.find(text)
        if (typeMatch != null) {
            val rawType = typeMatch.groupValues[1].lowercase()
            type = when {
                rawType.contains("лек") -> "Лекция"
                rawType.contains("практ") || rawType.contains("сем") -> "Практика / Семинар"
                rawType.contains("лаб") -> "Лабораторная"
                rawType.contains("зачет") -> "Зачет"
                rawType.contains("экзамен") -> "Экзамен"
                else -> typeMatch.value
            }
            text = text.removeRange(typeMatch.range).trim()
        }

        // 3. Teacher extraction: "Доцент Ковалева Е. А.", "Иванов И. О.", "Ломака А. А."
        val teacherRegex = Regex("""\b(?:(?:[Пп]рофессор|[Дд]оцент|[Пп]реподаватель|[Сс]т\.\s*преподаватель)\s+)?([А-ЯЁ][а-яё]+)\s+([А-ЯЁ]\.\s*[А-ЯЁ]\.?|[А-ЯЁ]\.)""")
        val teacherMatch = teacherRegex.find(text)
        var teacher: String? = null
        if (teacherMatch != null) {
            teacher = teacherMatch.value.trim()
            text = text.removeRange(teacherMatch.range).trim()
        }

        // Clean up remaining subject text
        val cleanedSubject = text.replace(Regex("""\s+"""), " ").trim()

        return Lesson(
            time = formattedTime ?: rawTime,
            subject = if (cleanedSubject.isNotBlank()) cleanedSubject else raw,
            teacher = teacher,
            room = room,
            type = type,
            rawText = raw,
            isEmptyWindow = false
        )
    }

    private fun formatTimeRange(rawTime: String): String {
        val match = Regex("""(\d{1,2})[.:](\d{2})\s*[-–—]\s*(\d{1,2})[.:](\d{2})""").find(rawTime)
        if (match != null) {
            val (h1, m1, h2, m2) = match.destructured
            return "${h1.padStart(2, '0')}.${m1} — ${h2.padStart(2, '0')}.${m2}"
        }
        return rawTime
    }
}