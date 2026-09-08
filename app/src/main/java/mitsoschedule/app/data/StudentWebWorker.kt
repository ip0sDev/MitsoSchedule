package mitsoschedule.app.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.CookieManager
import java.net.CookiePolicy
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class StudentWebWorker {

    private val client: OkHttpClient
    private val TAG = "StudentWebWorker"
    private val loginUrl = "https://student.mitso.by/login_stud.php"
    private val cabinetUrl = "https://student.mitso.by/"

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
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    suspend fun loginAndFetch(login: String, pass: String): Result<StudentCabinetData> {
        return withContext(Dispatchers.IO) {
            try {
                val formBody = FormBody.Builder()
                    .add("login", login.trim())
                    .add("password", pass.trim())
                    .build()

                val request = Request.Builder()
                    .url(loginUrl)
                    .post(formBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("Сервер вернул код ${response.code}"))
                    }

                    val html = response.body.string()

                    if (html.contains("Ошибка входа", ignoreCase = true) || html.contains("ввести корректные логин и пароль", ignoreCase = true)) {
                        return@withContext Result.failure(Exception("Неверный номер лицевого счета или пароль"))
                    }

                    // If the page doesn't have student data, try fetching main cabinet page with the session cookie
                    val finalHtml = if (!html.contains("Состояние лицевого счета", ignoreCase = true) && !html.contains("LMS Moodle", ignoreCase = true)) {
                        val getReq = Request.Builder().url(cabinetUrl).get().build()
                        client.newCall(getReq).execute().use { it.body.string() }
                    } else {
                        html
                    }

                    if (finalHtml.contains("Ошибка входа", ignoreCase = true)) {
                        return@withContext Result.failure(Exception("Неверный номер лицевого счета или пароль"))
                    }

                    val parsedData = parseCabinetHtml(finalHtml)
                    if (parsedData.fullName.isBlank() && parsedData.balance.isBlank() && parsedData.moodleLogin.isBlank()) {
                        return@withContext Result.failure(Exception("Не удалось извлечь данные кабинета"))
                    }

                    Result.success(parsedData)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loginAndFetch: ${e.message}", e)
                Result.failure(Exception("Ошибка соединения: ${e.localizedMessage ?: e.message}"))
            }
        }
    }

    fun parseCabinetHtml(html: String): StudentCabinetData {
        val doc = Jsoup.parse(html)
        val fullText = doc.text()

        // 1. Full name extraction (Look for 3 capitalized Russian words like "Корзун Денис Алексеевич")
        var fullName = ""
        val nameRegex = Regex("""\b([А-ЯЁ][а-яё]+)\s+([А-ЯЁ][а-яё]+)\s+([А-ЯЁ][а-яё]+)\b""")
        val studentHeaderIndex = fullText.indexOf("СТУДЕНТ", ignoreCase = true)
        if (studentHeaderIndex != -1) {
            val textAfterStudent = fullText.substring(studentHeaderIndex).take(200)
            val nameMatch = nameRegex.find(textAfterStudent)
            if (nameMatch != null) {
                fullName = nameMatch.value.trim()
            }
        }
        if (fullName.isBlank()) {
            val nameMatch = nameRegex.find(fullText)
            if (nameMatch != null && !nameMatch.value.contains("Состояние", ignoreCase = true)) {
                fullName = nameMatch.value.trim()
            }
        }

        // 2. Account date extraction
        var accountDate = ""
        val dateRegex = Regex("""(?:на конец дня|конец дня)\s*[:]?\s*(\d{2}[.-]\d{2}[.-]\d{4})""", RegexOption.IGNORE_CASE)
        val dateMatch = dateRegex.find(fullText)
        if (dateMatch != null) {
            accountDate = dateMatch.groupValues[1].trim()
        }

        // 3. Balance extraction
        var balance = "0.00"
        val balanceRegex = Regex("""Баланс\s*:\s*([^\s;]+(?:\s*[рpР][уuУ][бbБ])?)""", RegexOption.IGNORE_CASE)
        val balanceMatch = balanceRegex.find(fullText)
        if (balanceMatch != null) {
            balance = balanceMatch.groupValues[1].trim()
        }

        // 4. Main debt extraction
        var mainDebt = "0.00"
        val debtRegex = Regex("""Основной долг\s*:\s*([^\s;]+(?:\s*[рpР][уuУ][бbБ])?)""", RegexOption.IGNORE_CASE)
        val debtMatch = debtRegex.find(fullText)
        if (debtMatch != null) {
            mainDebt = debtMatch.groupValues[1].trim()
        }

        // 5. Penalty extraction
        var penalty = "0.00"
        val penaltyRegex = Regex("""Пеня[^\n\r:;]*:\s*([^\s;]+(?:\s*[рpР][уuУ][бbБ])?)""", RegexOption.IGNORE_CASE)
        val penaltyMatch = penaltyRegex.find(fullText)
        if (penaltyMatch != null) {
            penalty = penaltyMatch.groupValues[1].trim()
        }

        // 6. LMS Moodle Group
        var moodleGroup = ""
        val groupRegex = Regex("""Группа\s*:\s*([^\s<;]+)""", RegexOption.IGNORE_CASE)
        val groupMatch = groupRegex.find(fullText)
        if (groupMatch != null) {
            moodleGroup = groupMatch.groupValues[1].trim()
        }

        // 7. LMS Moodle Login
        var moodleLogin = ""
        val loginRegex = Regex("""Логин\s*:\s*([^\s<;]+)""", RegexOption.IGNORE_CASE)
        val loginMatch = loginRegex.find(fullText)
        if (loginMatch != null) {
            moodleLogin = loginMatch.groupValues[1].trim()
        }

        // 8. LMS Moodle Password
        var moodlePassword = ""
        val passRegex = Regex("""Пароль\s*:\s*([^\s<;]+)""", RegexOption.IGNORE_CASE)
        val passMatch = passRegex.find(fullText)
        if (passMatch != null) {
            moodlePassword = passMatch.groupValues[1].trim()
        }

        val currentTime = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())

        return StudentCabinetData(
            fullName = fullName,
            accountDate = accountDate,
            balance = balance,
            mainDebt = mainDebt,
            penalty = penalty,
            moodleGroup = moodleGroup,
            moodleLogin = moodleLogin,
            moodlePassword = moodlePassword,
            lastFetchedTime = currentTime
        )
    }
}
