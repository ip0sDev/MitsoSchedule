package by.iposdev.mitsotest

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.net.CookieManager
import java.net.CookiePolicy
import java.security.cert.X509Certificate // Раскомментировано
import javax.net.ssl.SSLContext             // Раскомментировано
import javax.net.ssl.TrustManager           // Раскомментировано
import javax.net.ssl.X509TrustManager       // Раскомментировано

// Data classes for structured schedule
data class Lesson(val text: String)
data class DaySchedule(val dayTitle: String, val lessons: List<Lesson>)

class WebWorker {

    private val client: OkHttpClient
    private val TAG = "WebWorker"

    init {
        Log.w(TAG, "Initializing OkHttpClient with INSECURE SSL configuration. DO NOT USE IN PRODUCTION.")

        // Создаем TrustManager, который не проверяет цепочки сертификатов (НЕБЕЗОПАСНО)
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        // Устанавливаем TrustManager, который доверяет всем
        val sslContext = SSLContext.getInstance("TLS") // Используем TLS
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        // Создаем SSLSocketFactory с нашим TrustManager, который доверяет всем
        val sslSocketFactory = sslContext.socketFactory

        val cookieManager = CookieManager()
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        val cookieJar = JavaNetCookieJar(cookieManager)

        client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager) // Применяем НЕБЕЗОПАСНЫЙ SSLSocketFactory
            .hostnameVerifier { _, _ -> true } // Отключаем проверку имени хоста (НЕБЕЗОПАСНО)
            .build()
    }

    private val mitsoSheduleURL = "https://apps.mitso.by/frontend/web/schedule/group-schedule"

    private fun parseScheduleString(scheduleText: String): List<DaySchedule> {
        val daySchedules = mutableListOf<DaySchedule>()
        if (scheduleText.isBlank()) {
            Log.w(TAG, "parseScheduleString: Input scheduleText is blank.")
            return emptyList()
        }

        val dayHeaderRegex = """(Понедельник|Вторник|Среда|Четверг|Пятница|Суббота|Воскресенье),\s*\d{1,2}\s+\p{L}+""".toRegex()
        val dayMatches = dayHeaderRegex.findAll(scheduleText).toList()

        if (dayMatches.isEmpty()) {
            Log.w(TAG, "parseScheduleString: No day headers found in schedule text.")
            Log.d(TAG, "parseScheduleString: Text without day headers (first 500 chars): ${scheduleText.take(500)}")
            return emptyList()
        }

        dayMatches.forEachIndexed { index, matchResult ->
            val dayTitle = matchResult.value.trim()
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
                val lessonParts = dayContent.split(Regex("(?=\\d{2}\\.\\d{2}-\\d{2}\\.\\d{2})"))
                                      .map { it.trim() }
                                      .filter { it.isNotEmpty() }
                if (lessonParts.isNotEmpty()) {
                    lessonParts.forEach { part -> lessons.add(Lesson(part)) }
                } else {
                    lessons.add(Lesson(dayContent)) // Fallback if no time patterns found
                }
            }
            daySchedules.add(DaySchedule(dayTitle, lessons))
        }
        Log.d(TAG, "parseScheduleString: Parsed ${daySchedules.size} days.")
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
                        Log.d(TAG, "getCSRFtoken - HTML received (first 500 chars): ${html.take(500)}")
                        val doc = Jsoup.parse(html)
                        val csrfTag = doc.select("meta[name=csrf-token]").first()
                        if (csrfTag == null) {
                            Log.w(TAG, "getCSRFtoken - CSRF meta tag not found by Jsoup.")
                            Log.d(TAG, "getCSRFtoken - All meta tags found: ${doc.select("meta").outerHtml()}")
                            return@withContext ""
                        }
                        Log.d(TAG, "getCSRFtoken - CSRF meta tag found: ${csrfTag.outerHtml()}")
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
                        Log.d(TAG, "sendRequest - HTML received (first 500 chars): ${html.take(500)}")
                        val doc = Jsoup.parse(html)
                        val scheduleDataString = doc.select("div.weekly-schedule").first()?.text()?.trim()
                        if (scheduleDataString.isNullOrEmpty()) {
                            Log.w(TAG, "sendRequest - Schedule data string not found or empty after parsing.")
                            Log.d(TAG, "sendRequest - Full HTML for schedule page: $html")
                            return@withContext emptyList()
                        }
                        Log.d(TAG, "sendRequest - Raw schedule string (first 500 chars): ${scheduleDataString.take(500)}")
                        parseScheduleString(scheduleDataString) // Call the parser
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