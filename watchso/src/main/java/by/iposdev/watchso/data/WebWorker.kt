package by.iposdev.watchso.data // Изменено

import android.content.Context // ADDED
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable // ДОБАВЛЕН ИМПОРТ
import okhttp3.Cache // ADDED
import okhttp3.FormBody
import okhttp3.JavaNetCookieJar
import okhttp3.CookieJar
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File // ADDED
import java.io.IOException
import java.net.CookieManager
import java.net.CookiePolicy
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit // Добавлен импорт
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

// Data classes for structured schedule
@Serializable // ДОБАВЛЕНА АННОТАЦИЯ
data class Lesson(val text: String)

@Serializable // ДОБАВЛЕНА АННОТАЦИЯ
data class DaySchedule(val dayTitle: String, val lessons: List<Lesson>)

class WebWorker(context: Context) { // ADDED context to constructor

    private val client: OkHttpClient
    // TODO: Rename to 'tag' to follow Kotlin conventions
    private val TAG = "WebWorker"

    init {
        Log.w(TAG, "FORCE UPDATE CHECK: Initializing OkHttpClient with INSECURE SSL configuration for watchso. DO NOT USE IN PRODUCTION.") // Added FORCE UPDATE CHECK

        try {
            val testHeaders = Headers.Builder().build()
            Log.d(TAG, "Successfully created testHeaders: $testHeaders")
        } catch (e: Throwable) {
            Log.e(TAG, "Error creating testHeaders", e)
        }

        try {
            val noCookiesInstance: CookieJar = CookieJar.NO_COOKIES
            Log.d(TAG, "Successfully referenced CookieJar.NO_COOKIES: $noCookiesInstance")
        } catch (e: Throwable) {
            Log.e(TAG, "Error referencing CookieJar.NO_COOKIES", e)
        }

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
        val actualCookieJarToUse = JavaNetCookieJar(cookieManager)

       // Setup cache
       val cacheSize = 10L * 1024 * 1024 // 10 MB
       val cacheDirectory = File(context.cacheDir, "http-cache")
       val cache = Cache(cacheDirectory, cacheSize)

        client = OkHttpClient.Builder()
            .cache(cache) // ADDED cache
            .cookieJar(actualCookieJarToUse)
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(30, TimeUnit.SECONDS) // Увеличен таймаут соединения
            .readTimeout(30, TimeUnit.SECONDS)    // Увеличен таймаут чтения
            .writeTimeout(30, TimeUnit.SECONDS)   // Увеличен таймаут записи
            .build()
    }

    private val mitsoSheduleURL = "https://apps.mitso.by/frontend/web/schedule/group-schedule"

    private fun parseScheduleString(scheduleText: String): List<DaySchedule> {
        val daySchedules = mutableListOf<DaySchedule>()
        if (scheduleText.isBlank()) {
            Log.w(TAG, "parseScheduleString: Input scheduleText is blank.")
            return emptyList()
        }
        val dayHeaderRegex = "(Понедельник|Вторник|Среда|Четверг|Пятница|Суббота|Воскресенье),\\s*\\d{1,2}\\s+\\p{L}+".toRegex()
        val dayMatches = dayHeaderRegex.findAll(scheduleText).toList()

        if (dayMatches.isEmpty()) {
            Log.w(TAG, "parseScheduleString: No day headers found in schedule text.")
            Log.d(TAG, "parseScheduleString: Text without day headers (first 500 chars): ${scheduleText.take(500)}")
            return emptyList()
        }

        dayMatches.forEachIndexed { index, matchResult ->
            val dayTitle = matchResult.value.trim()
            Log.d(TAG, "parseScheduleString: Found day: '$dayTitle'")
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
                // Regex для поиска отдельных записей о занятиях, например:
                // "08.00-9.25 (нет занятий)" или "09.35-11.00 Физкультура с/з Петров"
                // Regex ищет время HH.MM-HH.MM, за которым следуют детали (возможно, в скобках),
                // и продолжается до следующего времени или конца строки.
                // Небольшая правка regex: точка должна быть экранирована как \. а не просто .
                // И добавлена поддержка опционального пробела перед скобкой.
                val lessonEntryRegex = "(\\d{2}\\. \\d{2}-\\d{2}\\. \\d{2}(?:\\s*\\(.*?\\)|\\s+[^\\d(]*?(?=\\s*\\d{2}\\. \\d{2}|$)))".toRegex(RegexOption.IGNORE_CASE) // Добавлен IGNORE_CASE для большей гибкости с текстом типа "нет занятий"
                val lessonMatches = lessonEntryRegex.findAll(dayContent)

                if (!lessonMatches.any()) {
                    // Если regex не находит отдельных занятий, добавляем весь dayContent как раньше (запасной вариант)
                    lessons.add(Lesson(dayContent))
                    Log.w(TAG, "parseScheduleString: Day '$dayTitle' - No individual lessons found by regex, adding raw dayContent. Content: ${dayContent.take(100)}")
                } else {
                    lessonMatches.forEach { match ->
                        val lessonText = match.value.trim()
                        if (lessonText.isNotBlank()) {
                            lessons.add(Lesson(lessonText))
                            Log.d(TAG, "parseScheduleString: Day '$dayTitle' - Added lesson: '${lessonText.take(100)}'")
                        }
                    }
                }
            } else {
                 Log.w(TAG, "parseScheduleString: Day '$dayTitle' - dayContent is blank.")
            }
            Log.d(TAG, "parseScheduleString: Day '$dayTitle' - found ${lessons.size} lessons.")
            if (lessons.isNotEmpty()) {
                lessons.forEachIndexed { lessonIdx, lesson ->
                     Log.d(TAG, "parseScheduleString: Day '$dayTitle' - lesson ${lessonIdx + 1}: ${lesson.text.take(100)}'")
                }
            }
            daySchedules.add(DaySchedule(dayTitle, lessons))
        }
        Log.i(TAG, "parseScheduleString: Parsed ${daySchedules.size} days successfully.")
        return daySchedules
    }

    suspend fun getCSRFtoken(): String {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Attempting to get CSRF token from: $mitsoSheduleURL")
            val request = Request.Builder().url(mitsoSheduleURL).get().build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "getCSRFtoken - Unsuccessful response: ${response.code} ${response.message}")
                        return@withContext ""
                    }
                    val html = response.body?.string()
                    if (html != null) {
                        val doc = Jsoup.parse(html)
                        val csrfTag = doc.select("meta[name=csrf-token]").first()
                        if (csrfTag == null) {
                            Log.w(TAG, "getCSRFtoken - CSRF meta tag not found by Jsoup.")
                            Log.d(TAG, "getCSRFtoken - All meta tags found: ${doc.select("meta").outerHtml()}")
                            return@withContext ""
                        }
                        val tokenValue = csrfTag.attr("content")
                        Log.d(TAG, "getCSRFtoken - Extracted token value: '$tokenValue'")
                        if (tokenValue.isNullOrEmpty()) {
                            Log.w(TAG, "getCSRFtoken - CSRF token value is null or empty.")
                            return@withContext ""
                        }
                        tokenValue
                    } else {
                        Log.w(TAG, "getCSRFtoken - Empty response body")
                        ""
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "getCSRFtoken - IOException: ${e.message}", e)
                ""
            } catch (e: Exception) {
                Log.e(TAG, "getCSRFtoken - Exception: ${e.message}", e)
                ""
            }
        }
    }

    suspend fun sendRequest(): List<DaySchedule> { // Return type changed
        return withContext(Dispatchers.IO) {
            val token = getCSRFtoken()
            if (token.isEmpty()) {
                Log.e(TAG, "sendRequest - CSRF token is empty, cannot proceed.")
                return@withContext emptyList() // Return empty list on error
            }
            Log.d(TAG, "sendRequest - Using CSRF token: $token")
            val formBody = FormBody.Builder()
                .add("_csrf-frontend", token)
                .add("ScheduleSearch[fak]", "E`konomicheskij")
                .add("ScheduleSearch[form]", "Dnevnaya")
                .add("ScheduleSearch[kurse]", "2 kurs")
                .add("ScheduleSearch[group_class]", "2423 UIR")
                .add("ScheduleSearch[week]", "0")
                .build()
            val request = Request.Builder().url(mitsoSheduleURL).post(formBody).build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "sendRequest - Unsuccessful response: ${response.code} ${response.message}")
                        response.body?.string()?.let { Log.e(TAG, "sendRequest - Error body: $it") }
                        return@withContext emptyList()
                    }
                    val html = response.body?.string()
                    if (html != null) {
                        val doc = Jsoup.parse(html)
                        val scheduleDataString = doc.select("div.weekly-schedule").first()?.text()?.trim()
                        if (scheduleDataString.isNullOrEmpty()) {
                            Log.w(TAG, "sendRequest - Schedule data string not found or empty after parsing.")
                            Log.d(TAG, "sendRequest - Full HTML for schedule page (this is likely the form page again): $html")
                            return@withContext emptyList()
                        }
                        Log.d(TAG, "sendRequest - Raw schedule string (first 500 chars): ${scheduleDataString.take(500)}")
                        parseScheduleString(scheduleDataString)
                    } else {
                        Log.w(TAG, "sendRequest - Empty response body for schedule request")
                        emptyList()
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "sendRequest - IOException: ${e.message}", e)
                emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "sendRequest - Exception: ${e.message}", e)
                emptyList()
            }
        }
    }
}
