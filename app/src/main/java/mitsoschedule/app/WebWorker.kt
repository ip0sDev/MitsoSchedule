package mitsoschedule.app

import android.util.Log
import mitsoschedule.app.data.DaySchedule
import mitsoschedule.app.data.DepDropResponse
import mitsoschedule.app.data.Lesson
import mitsoschedule.app.data.OptionItem
import mitsoschedule.app.data.SubgroupInfo
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
            val mergedLessons = groupLessonsByTime(cleanedLessons)
            if (mergedLessons.isNotEmpty()) {
                daySchedules.add(DaySchedule(dayTitle = "$dayTitle, $dateSubtitle", dateSubtitle = dateSubtitle, lessons = mergedLessons))
            }
        }

        return daySchedules
    }

    fun groupLessonsByTime(lessons: List<Lesson>): List<Lesson> = Companion.groupLessonsByTime(lessons)

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

        val text = if (timeMatch != null) {
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

        // Check if text has multiple subgroups separated by " / " or " // "
        if (text.contains(" / ") || text.contains(" // ")) {
            val parts = text.split(Regex("""\s*/\s*|\s*//\s*"""))
            val hasMultipleSubgroups = parts.size > 1 && parts.any { p ->
                LEADING_SUBGROUP_REGEX.containsMatchIn(p) || INLINE_SUBGROUP_REGEXES.any { it.containsMatchIn(p) }
            }

            if (hasMultipleSubgroups) {
                val extractedSubgroups = parts.mapIndexed { index, part ->
                    val (sgName, afterSg) = extractSubgroupFromText(part)
                    val (room, afterRoom) = extractRoomFromText(afterSg)
                    val (teacher, afterTeacher) = extractTeacherFromText(afterRoom)
                    val (type, _) = extractTypeFromText(afterTeacher)
                    SubgroupInfo(
                        subgroup = sgName ?: "${index + 1} подгруппа",
                        teacher = teacher,
                        room = room,
                        subject = null,
                        type = type
                    )
                }.sortedBy { it.subgroup }

                val (commonType, _) = extractTypeFromText(text)
                return Lesson(
                    time = formattedTime ?: rawTime,
                    subject = cleanSubjectTitle(text),
                    type = commonType,
                    rawText = raw,
                    isEmptyWindow = false,
                    subgroups = extractedSubgroups
                )
            }
        }

        val (subgroup, textAfterSg) = extractSubgroupFromText(text)
        val (type, textAfterType) = extractTypeFromText(textAfterSg)
        val (teacher, textAfterTeacher) = extractTeacherFromText(textAfterType)
        val (room, textAfterRoom) = extractRoomFromText(textAfterTeacher)
        val cleanedSubject = cleanSubjectTitle(textAfterRoom)

        return Lesson(
            time = formattedTime ?: rawTime,
            subject = if (cleanedSubject.isNotBlank()) cleanedSubject else raw,
            teacher = teacher,
            room = room,
            type = type,
            rawText = raw,
            isEmptyWindow = false,
            subgroup = subgroup
        )
    }

    companion object {
        val TYPE_REGEX = Regex(
            """\((лек|практ|сем|лаб|консультация|зачет|экзамен|практ/сем|сем/практ)\)""",
            RegexOption.IGNORE_CASE
        )

        val TEACHER_REGEX = Regex(
            """\b(?:(?:[Пп]рофессор|[Дд]оцент|[Пп]реподаватель|[Сс]т\.\s*преподаватель)\s+)?([А-ЯЁ][а-яё]+)\s+([А-ЯЁ]\.\s*[А-ЯЁ]\.?|[А-ЯЁ]\.)"""
        )

        val LEADING_SUBGROUP_REGEX = Regex("""^([1-4])\.\s*""")

        val INLINE_SUBGROUP_REGEXES = listOf(
            Regex("""(?:\(?\b([1-4])\s*(?:[-–—]\s*(?:я|ая))?\s*(?:п/?г|п\.г\.|подгр(?:уппа|\.)?|группа)\b\)?)""", RegexOption.IGNORE_CASE),
            Regex("""(?:\(?\b(?:п/?г|п\.г\.|подгр(?:уппа|\.)?|группа)\s*([1-4])\b\)?)""", RegexOption.IGNORE_CASE)
        )

        val EXPLICIT_ROOM_REGEX = Regex(
            """(?i)\b(?:ауд\.|каб\.|аудитория|кабинет|зал|с/з|спортзал|актовый\s*зал)\s*([0-9а-яА-Яa-zA-Z/-]+(?:\s*-\s*[0-9а-яА-Яa-zA-Z/-]+)?(?:\s*\([а-яА-Яa-zA-Z0-9.\s]+\))?)"""
        )

        // Matches 63 (к), 62 (k), 63 (к.), 51-52 (к) anywhere in text
        val PAREN_ROOM_REGEX = Regex(
            """\b(\d{1,3}(?:\s*-\s*\d{1,3})?\s*\([а-яА-Яa-zA-Z0-9.\s]+\))"""
        )

        // Matches 51-52 or similar ranges
        val RANGE_ROOM_REGEX = Regex(
            """(?<=\s|^)(\d{1,3}\s*-\s*\d{1,3})(?=\s|$|[.,;/-])"""
        )

        val SPECIAL_ROOM_WORDS_REGEX = Regex(
            """(?i)\b(спортзал|с/з|актовый\s*зал)\b"""
        )

        val STANDALONE_NUMBER_ROOM_REGEX = Regex(
            """(?<=\s|^)(\d{2,3})(?=\s*$|\s*[А-ЯЁ][а-яё]+|\s*[(/])"""
        )

        fun extractSubgroupFromText(text: String): Pair<String?, String> {
            val current = text.trim()
            val leadingMatch = LEADING_SUBGROUP_REGEX.find(current)
            if (leadingMatch != null) {
                val num = leadingMatch.groupValues[1]
                return Pair("$num подгруппа", current.removeRange(leadingMatch.range).trim())
            }

            for (regex in INLINE_SUBGROUP_REGEXES) {
                val match = regex.find(current)
                if (match != null) {
                    val groupNum = match.groupValues.getOrNull(1)?.ifBlank { null }
                        ?: match.groupValues.getOrNull(2)?.ifBlank { null }
                    val subgroup = if (groupNum != null) "$groupNum подгруппа" else match.value.trim().removeSurrounding("(", ")")
                    val cleaned = (current.substring(0, match.range.first) + " " + current.substring(match.range.last + 1)).trim()
                    return Pair(subgroup, cleaned)
                }
            }

            return Pair(null, current)
        }

        fun extractRoomFromText(text: String): Pair<String?, String> {
            val current = text.trim()

            // 1. Explicit room keyword like "ауд. 312", "каб. 51-52"
            val explicitMatch = EXPLICIT_ROOM_REGEX.find(current)
            if (explicitMatch != null) {
                val rawVal = explicitMatch.value.trim()
                val cleaned = (current.substring(0, explicitMatch.range.first) + " " + current.substring(explicitMatch.range.last + 1)).trim()
                return Pair(rawVal, cleaned)
            }

            // 2. Special room words without number: "спортзал", "с/з", "актовый зал"
            val specialMatch = SPECIAL_ROOM_WORDS_REGEX.find(current)
            if (specialMatch != null) {
                val rawVal = specialMatch.value.trim()
                val cleaned = (current.substring(0, specialMatch.range.first) + " " + current.substring(specialMatch.range.last + 1)).trim()
                return Pair(rawVal, cleaned)
            }

            // 3. Parenthesis room like "63 (к)", "62 (к)", "63 (k)"
            val parenMatch = PAREN_ROOM_REGEX.find(current)
            if (parenMatch != null) {
                val rawVal = parenMatch.value.trim()
                val formatted = "ауд. $rawVal"
                val cleaned = (current.substring(0, parenMatch.range.first) + " " + current.substring(parenMatch.range.last + 1)).trim()
                return Pair(formatted, cleaned)
            }

            // 4. Range room like "51-52"
            val rangeMatch = RANGE_ROOM_REGEX.find(current)
            if (rangeMatch != null) {
                val rawVal = rangeMatch.value.trim()
                val formatted = "ауд. $rawVal"
                val cleaned = (current.substring(0, rangeMatch.range.first) + " " + current.substring(rangeMatch.range.last + 1)).trim()
                return Pair(formatted, cleaned)
            }

            // 5. Standalone 2-3 digit number room
            val numMatch = STANDALONE_NUMBER_ROOM_REGEX.find(current)
            if (numMatch != null) {
                val rawVal = numMatch.value.trim()
                val formatted = "ауд. $rawVal"
                val cleaned = (current.substring(0, numMatch.range.first) + " " + current.substring(numMatch.range.last + 1)).trim()
                return Pair(formatted, cleaned)
            }

            return Pair(null, current)
        }

        fun extractTypeFromText(text: String): Pair<String?, String> {
            val match = TYPE_REGEX.find(text) ?: return Pair(null, text)
            val rawType = match.groupValues[1].lowercase()
            val formatted = when {
                rawType.contains("лек") -> "Лекция"
                rawType.contains("практ") || rawType.contains("сем") -> "Практика / Семинар"
                rawType.contains("лаб") -> "Лабораторная"
                rawType.contains("зачет") -> "Зачет"
                rawType.contains("экзамен") -> "Экзамен"
                else -> match.value
            }
            val cleaned = (text.substring(0, match.range.first) + " " + text.substring(match.range.last + 1)).trim()
            return Pair(formatted, cleaned)
        }

        fun extractTeacherFromText(text: String): Pair<String?, String> {
            val match = TEACHER_REGEX.find(text) ?: return Pair(null, text)
            val teacher = match.value.trim()
            val cleaned = (text.substring(0, match.range.first) + " " + text.substring(match.range.last + 1)).trim()
            return Pair(teacher, cleaned)
        }

        fun cleanSubjectTitle(subject: String): String {
            if (subject.isBlank()) return ""
            val parts = subject.split(Regex("""\s*/\s*|\s*//\s*"""))
            val cleanedParts = parts.map { part ->
                var s = part.trim()
                s = s.replace(LEADING_SUBGROUP_REGEX, "")
                for (regex in INLINE_SUBGROUP_REGEXES) {
                    s = s.replace(regex, "")
                }
                s = s.replace(EXPLICIT_ROOM_REGEX, "")
                s = s.replace(PAREN_ROOM_REGEX, "")
                s = s.replace(RANGE_ROOM_REGEX, "")
                s = s.replace(SPECIAL_ROOM_WORDS_REGEX, "")
                s = s.replace(TYPE_REGEX, "")
                s = s.replace(TEACHER_REGEX, "")
                s = s.replace(Regex("""^[\s.,;/-]+|[\s.,;/-]+$"""), "")
                s.replace(Regex("""\s+"""), " ").trim()
            }.filter { it.isNotBlank() }

            val distinct = cleanedParts.distinctBy { it.lowercase() }
            return when {
                distinct.isEmpty() -> subject.replace(LEADING_SUBGROUP_REGEX, "").trim()
                distinct.size == 1 -> distinct.first()
                else -> distinct.joinToString(" / ")
            }
        }

        fun normalizeLesson(lesson: Lesson): Lesson {
            if (lesson.isEmptyWindow) return lesson

            val cleanedSubject = cleanSubjectTitle(lesson.subject)

            // Case 1: Subgroups already present
            if (lesson.subgroups.isNotEmpty()) {
                val fullContext = "${lesson.rawText} ${lesson.subject}"
                val normalizedSubgroups = lesson.subgroups.map { sg ->
                    var room = sg.room
                    val teacher = sg.teacher

                    if (room.isNullOrBlank()) {
                        val sgNum = Regex("""\b([1-4])\b""").find(sg.subgroup)?.groupValues?.get(1)
                        if (sgNum != null) {
                            val chunkMatch = Regex("""(?:^|\s|/)$sgNum[.\s][^/]*""").find(fullContext)
                            if (chunkMatch != null) {
                                val (extractedRoom, _) = extractRoomFromText(chunkMatch.value)
                                if (extractedRoom != null) {
                                    room = extractedRoom
                                }
                            }
                        }
                    }

                    if (room.isNullOrBlank()) {
                        val (extractedRoom, _) = extractRoomFromText(fullContext)
                        room = extractedRoom
                    }

                    sg.copy(
                        room = room,
                        teacher = teacher,
                        subject = if (sg.subject != null) cleanSubjectTitle(sg.subject) else null
                    )
                }.sortedBy { it.subgroup }

                return lesson.copy(
                    subject = cleanedSubject,
                    room = null,
                    teacher = null,
                    subgroups = normalizedSubgroups
                )
            }

            // Case 2: Subgroups not structured yet, but text or subject contains multiple subgroups
            val fullRaw = if (lesson.rawText.isNotBlank()) lesson.rawText else lesson.subject
            val rawWithoutTime = fullRaw.replace(Regex("""^(\d{1,2}[.:]\d{2}\s*[-–—]\s*\d{1,2}[.:]\d{2})\s*"""), "").trim()

            val hasSubgroup1 = Regex("""(?<=\s|^)1\.\s+|(?<=\s|^)1\s*п/?г""", RegexOption.IGNORE_CASE).containsMatchIn(rawWithoutTime)
            val hasSubgroup2 = Regex("""(?<=\s|^)2\.\s+|(?<=\s|^)2\s*п/?г""", RegexOption.IGNORE_CASE).containsMatchIn(rawWithoutTime)
            val hasMultipleSubgroups = (hasSubgroup1 && hasSubgroup2) ||
                    (lesson.subject.contains(" / ") && (lesson.subject.contains("63") || lesson.subject.contains("62") || lesson.subject.contains("ауд")))

            if (hasMultipleSubgroups) {
                val parts = if (rawWithoutTime.contains(" / ")) {
                    rawWithoutTime.split(Regex("""\s*/\s*"""))
                } else if (rawWithoutTime.contains("\n")) {
                    rawWithoutTime.split("\n")
                } else {
                    rawWithoutTime.split(Regex("""(?<=\s|^)(?=[1-4]\.\s+)""")).filter { it.isNotBlank() }
                }

                if (parts.size > 1) {
                    val extractedSubgroups = parts.mapIndexed { index, part ->
                        val (sgName, afterSg) = extractSubgroupFromText(part)
                        val (room, afterRoom) = extractRoomFromText(afterSg)
                        val (teacher, _) = extractTeacherFromText(afterRoom)
                        val (type, _) = extractTypeFromText(part)
                        SubgroupInfo(
                            subgroup = sgName ?: "${index + 1} подгруппа",
                            teacher = teacher,
                            room = room,
                            subject = null,
                            type = type ?: lesson.type
                        )
                    }.sortedBy { it.subgroup }

                    return lesson.copy(
                        subject = cleanedSubject,
                        room = null,
                        teacher = null,
                        subgroups = extractedSubgroups
                    )
                }
            }

            // Case 3: Single lesson without subgroups
            var singleRoom = lesson.room
            if (singleRoom.isNullOrBlank()) {
                val (foundRoom, _) = extractRoomFromText("${lesson.subject} ${lesson.rawText}")
                singleRoom = foundRoom
            }

            var singleSubgroup = lesson.subgroup
            if (singleSubgroup == null) {
                val (foundSubgroup, _) = extractSubgroupFromText("${lesson.subject} ${lesson.rawText}")
                singleSubgroup = foundSubgroup
            }

            return lesson.copy(
                subject = cleanedSubject,
                room = singleRoom,
                subgroup = singleSubgroup
            )
        }

        fun groupLessonsByTime(lessons: List<Lesson>): List<Lesson> {
            if (lessons.isEmpty()) return emptyList()

            val normalizedList = lessons.map { normalizeLesson(it) }
            val result = mutableListOf<Lesson>()
            var i = 0
            while (i < normalizedList.size) {
                val current = normalizedList[i]
                if (current.isEmptyWindow || current.time.isNullOrBlank()) {
                    result.add(current)
                    i++
                    continue
                }

                val sameTimeLessons = mutableListOf(current)
                var j = i + 1
                while (j < normalizedList.size && !normalizedList[j].isEmptyWindow && normalizedList[j].time == current.time) {
                    sameTimeLessons.add(normalizedList[j])
                    j++
                }

                if (sameTimeLessons.size == 1) {
                    if (current.subgroup != null && current.subgroups.isEmpty()) {
                        result.add(
                            current.copy(
                                subgroups = listOf(
                                    SubgroupInfo(
                                        subgroup = current.subgroup,
                                        teacher = current.teacher,
                                        room = current.room,
                                        subject = null,
                                        type = current.type
                                    )
                                )
                            )
                        )
                    } else {
                        result.add(current)
                    }
                    i++
                } else {
                    val subgroupList = sameTimeLessons.flatMap { item ->
                        if (item.subgroups.isNotEmpty()) {
                            item.subgroups
                        } else {
                            listOf(
                                SubgroupInfo(
                                    subgroup = item.subgroup ?: "Подгруппа",
                                    teacher = item.teacher,
                                    room = item.room,
                                    subject = null,
                                    type = item.type
                                )
                            )
                        }
                    }.sortedBy { it.subgroup }

                    val commonSubject = cleanSubjectTitle(sameTimeLessons.joinToString(" / ") { it.subject })
                    val commonType = sameTimeLessons.firstOrNull { !it.type.isNullOrBlank() }?.type

                    val mergedLesson = Lesson(
                        time = current.time,
                        subject = commonSubject,
                        type = commonType,
                        teacher = null,
                        room = null,
                        rawText = sameTimeLessons.joinToString("\n") { it.rawText },
                        isEmptyWindow = false,
                        subgroup = null,
                        subgroups = subgroupList
                    )
                    result.add(mergedLesson)
                    i = j
                }
            }
            return result
        }

        fun formatTimeRange(rawTime: String): String {
            val match = Regex("""(\d{1,2})[.:](\d{2})\s*[-–—]\s*(\d{1,2})[.:](\d{2})""").find(rawTime)
            if (match != null) {
                val (h1, m1, h2, m2) = match.destructured
                return "${h1.padStart(2, '0')}.${m1} — ${h2.padStart(2, '0')}.${m2}"
            }
            return rawTime
        }
    }
}