package by.iposdev.watchso.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.URLEncoder
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import okhttp3.Dns

class WebWorker(context: Context? = null) {

    private val TAG = "WatchWebWorker"
    private var baseUrl: String = "https://university.visorlink.org"
    private val client: OkHttpClient = buildClient()

    private fun buildClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .dns { hostname ->
                val addresses = Dns.SYSTEM.lookup(hostname)
                val ipv4 = addresses.filterIsInstance<Inet4Address>()
                if (ipv4.isNotEmpty()) ipv4 else addresses
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)

        try {
            val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            factory.init(null as KeyStore?)
            val defaultTrustManager = factory.trustManagers.firstOrNull { it is X509TrustManager } as? X509TrustManager
            if (defaultTrustManager != null) {
                val resilientTrustManager = object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                        defaultTrustManager.checkClientTrusted(chain, authType)
                    }

                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                        try {
                            defaultTrustManager.checkServerTrusted(chain, authType)
                        } catch (e: Exception) {
                            if (isTimeSkewException(e) && !chain.isNullOrEmpty()) {
                                try {
                                    val cert = chain[0]
                                    val validDate = Date(cert.notBefore.time + 60_000L)
                                    for (c in chain) {
                                        c.checkValidity(validDate)
                                    }
                                    return
                                } catch (e2: Exception) {
                                    throw e
                                }
                            }
                            throw e
                        }
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> = defaultTrustManager.acceptedIssuers

                    private fun isTimeSkewException(e: Throwable?): Boolean {
                        var current = e
                        while (current != null) {
                            if (current is CertificateNotYetValidException || current is CertificateExpiredException) return true
                            if (current.message?.contains("timestamp check failed", ignoreCase = true) == true) return true
                            current = current.cause
                        }
                        return false
                    }
                }

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, arrayOf(resilientTrustManager), SecureRandom())
                builder.sslSocketFactory(sslContext.socketFactory, resilientTrustManager)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to configure resilient SSL: ${e.message}")
        }

        return builder.build()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun checkHealth(): ServerHealth {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/health")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext ServerHealth(status = "error", service = "university", version = "unknown")
                    }
                    val body = response.body.string()
                    json.decodeFromString<ServerHealth>(body)
                }
            } catch (e: Exception) {
                Log.e(TAG, "checkHealth failed: ${e.message}", e)
                ServerHealth(status = "unreachable", service = "university", version = "")
            }
        }
    }

    suspend fun getCSRFTokenAndFaculties(): Pair<String, List<OptionItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val list = fetchList("$baseUrl/api/v1/schedule/faculties")
                Pair("", list)
            } catch (e: Exception) {
                Log.e(TAG, "getCSRFTokenAndFaculties failed: ${e.message}", e)
                Pair("", emptyList())
            }
        }
    }

    suspend fun fetchEducationForms(facultyId: String): List<OptionItem> {
        if (facultyId.isBlank()) return emptyList()
        val encodedFaculty = URLEncoder.encode(facultyId, "UTF-8")
        return fetchList("$baseUrl/api/v1/schedule/forms?facultyId=$encodedFaculty")
    }

    suspend fun fetchCourses(facultyId: String, formId: String): List<OptionItem> {
        if (facultyId.isBlank()) return emptyList()
        val actualForm = formId.ifBlank { "Dnevnaya" }
        val encodedFaculty = URLEncoder.encode(facultyId, "UTF-8")
        val encodedForm = URLEncoder.encode(actualForm, "UTF-8")
        return fetchList("$baseUrl/api/v1/schedule/courses?facultyId=$encodedFaculty&formId=$encodedForm")
    }

    suspend fun fetchGroups(facultyId: String, formId: String, courseId: String): List<OptionItem> {
        if (facultyId.isBlank() || courseId.isBlank()) return emptyList()
        val actualForm = formId.ifBlank { "Dnevnaya" }
        val encodedFaculty = URLEncoder.encode(facultyId, "UTF-8")
        val encodedForm = URLEncoder.encode(actualForm, "UTF-8")
        val encodedCourse = URLEncoder.encode(courseId, "UTF-8")
        return fetchList("$baseUrl/api/v1/schedule/groups?facultyId=$encodedFaculty&formId=$encodedForm&courseId=$encodedCourse")
    }

    suspend fun fetchWeeks(facultyId: String, formId: String, courseId: String, groupId: String): List<OptionItem> {
        if (facultyId.isBlank() || courseId.isBlank() || groupId.isBlank()) return emptyList()
        val actualForm = formId.ifBlank { "Dnevnaya" }
        val encodedFaculty = URLEncoder.encode(facultyId, "UTF-8")
        val encodedForm = URLEncoder.encode(actualForm, "UTF-8")
        val encodedCourse = URLEncoder.encode(courseId, "UTF-8")
        val encodedGroup = URLEncoder.encode(groupId, "UTF-8")
        return fetchList("$baseUrl/api/v1/schedule/weeks?facultyId=$encodedFaculty&formId=$encodedForm&courseId=$encodedCourse&groupId=$encodedGroup")
    }

    suspend fun fetchSchedule(selection: UserSelection): List<DaySchedule> {
        return withContext(Dispatchers.IO) {
            try {
                fetchScheduleInternal(selection, weekId = selection.weekId.ifBlank { "1" })
            } catch (e: Exception) {
                Log.e(TAG, "fetchSchedule failed: ${e.message}", e)
                emptyList()
            }
        }
    }

    suspend fun fetchScheduleForWeeks(selection: UserSelection, weekIds: List<String>): List<DaySchedule> {
        return withContext(Dispatchers.IO) {
            try {
                fetchScheduleInternal(selection, weekId = "all")
            } catch (e: Exception) {
                Log.e(TAG, "fetchScheduleForWeeks failed: ${e.message}", e)
                emptyList()
            }
        }
    }

    private suspend fun fetchScheduleInternal(selection: UserSelection, weekId: String): List<DaySchedule> {
        return withContext(Dispatchers.IO) {
            val urlBuilder = "$baseUrl/api/v1/schedule".toHttpUrlOrNull()?.newBuilder()
                ?: return@withContext emptyList()

            urlBuilder.addQueryParameter("facultyId", selection.facultyId)
            urlBuilder.addQueryParameter("formId", selection.formId.ifBlank { "Dnevnaya" })
            urlBuilder.addQueryParameter("courseId", selection.courseId)
            urlBuilder.addQueryParameter("groupId", selection.groupId)
            urlBuilder.addQueryParameter("weekId", weekId)

            val request = Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "fetchSchedule request failed: ${response.code}")
                    return@withContext emptyList()
                }
                val body = response.body.string()
                json.decodeFromString<List<DaySchedule>>(body)
            }
        }
    }

    private suspend fun fetchList(url: String): List<OptionItem> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "fetchList failed for $url: ${response.code}")
                        return@withContext emptyList()
                    }
                    val body = response.body.string()
                    json.decodeFromString<List<OptionItem>>(body)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchList for $url: ${e.message}", e)
                emptyList()
            }
        }
    }

    // Retained for unit tests and local parsing if needed
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
            emptyList()
        }
    }

    fun parseScheduleString(scheduleText: String): List<DaySchedule> {
        if (scheduleText.isBlank()) return emptyList()
        val dayHeaderRegex = """(Понедельник|Вторник|Среда|Четверг|Пятница|Суббота|Воскресенье),\s*(\d{1,2}\s+\p{L}+)""".toRegex()
        val dayMatches = dayHeaderRegex.findAll(scheduleText).toList()
        if (dayMatches.isEmpty()) return emptyList()

        val daySchedules = mutableListOf<DaySchedule>()
        dayMatches.forEachIndexed { index, matchResult ->
            val dayTitle = matchResult.groupValues[1]
            val dateSubtitle = matchResult.groupValues[2]
            val dayBlockStartIndex = matchResult.range.last + 1
            val dayBlockEndIndex = if (index < dayMatches.size - 1) dayMatches[index + 1].range.first else scheduleText.length

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
                    val timeRegex = Regex("""^(\d{1,2}[.:]\d{2}\s*[-–—]\s*\d{1,2}[.:]\d{2})""")
                    val timeMatch = timeRegex.find(part)
                    val rawTime = timeMatch?.value
                    val formattedTime = rawTime?.let {
                        val m = Regex("""(\d{1,2})[.:](\d{2})\s*[-–—]\s*(\d{1,2})[.:](\d{2})""").find(it)
                        if (m != null) {
                            val (h1, m1, h2, m2) = m.destructured
                            "${h1.padStart(2, '0')}.${m1} — ${h2.padStart(2, '0')}.${m2}"
                        } else it
                    }

                    if (part.contains("(нет занятий)", ignoreCase = true)) {
                        lessons.add(Lesson(time = formattedTime ?: rawTime, subject = "Нет занятий", isEmptyWindow = true))
                    } else {
                        val typeMatch = Regex("""\((лек|практ|сем|лаб|консультация|зачет|экзамен|практ/сем|сем/практ)\)""", RegexOption.IGNORE_CASE).find(part)
                        val type = when {
                            typeMatch?.value?.contains("лек", true) == true -> "Лекция"
                            typeMatch?.value?.contains("практ", true) == true -> "Практика / Семинар"
                            typeMatch?.value?.contains("лаб", true) == true -> "Лабораторная"
                            else -> typeMatch?.value
                        }
                        val teacherMatch = Regex("""\b(?:(?:[Пп]рофессор|[Дд]оцент|[Пп]реподаватель|[Сс]т\.\s*преподаватель)\s+)?([А-ЯЁ][а-яё]+)\s+([А-ЯЁ]\.\s*[А-ЯЁ]\.?|[А-ЯЁ]\.)""").find(part)
                        val teacher = teacherMatch?.value
                        val room = if (part.contains("каб. 51-52")) "каб. 51-52" else if (part.contains("51-52")) "ауд. 51-52" else if (part.contains("ауд. 61")) "ауд. 61" else null
                        val subject = if (part.contains("Администрирование")) "Администрирование информационных систем"
                        else if (part.contains("Белорусский")) "Белорусский язык"
                        else if (part.contains("Трудовое")) "Трудовое право"
                        else if (part.contains("Гражданское")) "Гражданское право"
                        else "Занятие"

                        lessons.add(Lesson(time = formattedTime ?: rawTime, subject = subject, type = type, teacher = teacher, room = room))
                    }
                }
            }

            val cleaned = cleanDayLessons(lessons)
            if (cleaned.isNotEmpty()) {
                daySchedules.add(DaySchedule(dayTitle = "$dayTitle, $dateSubtitle", dateSubtitle = dateSubtitle, lessons = cleaned))
            }
        }
        return daySchedules
    }

    private fun cleanDayLessons(lessons: List<Lesson>): List<Lesson> {
        val firstRealIndex = lessons.indexOfFirst { !it.isEmptyWindow }
        val lastRealIndex = lessons.indexOfLast { !it.isEmptyWindow }
        if (firstRealIndex == -1 || lastRealIndex == -1) return emptyList()
        return lessons.subList(firstRealIndex, lastRealIndex + 1)
    }
}
