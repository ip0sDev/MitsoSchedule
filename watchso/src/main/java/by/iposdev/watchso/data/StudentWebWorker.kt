package by.iposdev.watchso.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.Inet4Address
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

class StudentWebWorker(
    private var baseUrl: String = "https://university.visorlink.org"
) {

    private val TAG = "WatchStudentWebWorker"
    private val client: OkHttpClient = buildClient()

    private fun buildClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .dns { hostname ->
                val addresses = Dns.SYSTEM.lookup(hostname)
                val ipv4 = addresses.filterIsInstance<Inet4Address>()
                if (ipv4.isNotEmpty()) ipv4 else addresses
            }
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)

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

    fun updateBaseUrl(newUrl: String) {
        baseUrl = newUrl.trimEnd('/')
    }

    suspend fun loginAndFetch(login: String, pass: String): Result<StudentCabinetData> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonPayload = """{"login":${Json.encodeToString(login.trim())},"password":${Json.encodeToString(pass.trim())}}"""
                val body = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url("$baseUrl/api/v1/student/cabinet")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseText = response.body.string()

                    if (response.code == 401) {
                        return@withContext Result.failure(Exception("Неверный номер лицевого счета или пароль"))
                    }

                    if (response.code == 429) {
                        val errorMsg = try {
                            json.parseToJsonElement(responseText).jsonObject["error"]?.jsonPrimitive?.content
                        } catch (e: Exception) { null }
                        return@withContext Result.failure(
                            Exception(errorMsg ?: "Превышен лимит запросов. Подождите немного.")
                        )
                    }

                    if (!response.isSuccessful) {
                        val errorMsg = try {
                            json.parseToJsonElement(responseText).jsonObject["error"]?.jsonPrimitive?.content
                        } catch (e: Exception) { null }
                        return@withContext Result.failure(
                            Exception(errorMsg ?: "Сервер вернул код ${response.code}")
                        )
                    }

                    val data = json.decodeFromString<StudentCabinetData>(responseText)
                    Result.success(data)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loginAndFetch: ${e.message}", e)
                Result.failure(Exception("Ошибка соединения с сервером: ${e.localizedMessage ?: e.message}"))
            }
        }
    }

    fun parseCabinetHtml(html: String): StudentCabinetData {
        val fullText = html.replace(Regex("<[^>]*>"), " ")
        var fullName = ""
        val nameRegex = Regex("""\b([А-ЯЁ][а-яё]+)\s+([А-ЯЁ][а-яё]+)\s+([А-ЯЁ][а-яё]+)\b""")
        val studentHeaderIndex = fullText.indexOf("СТУДЕНТ", ignoreCase = true)
        if (studentHeaderIndex != -1) {
            val textAfterStudent = fullText.substring(studentHeaderIndex).take(200)
            val nameMatch = nameRegex.find(textAfterStudent)
            if (nameMatch != null) fullName = nameMatch.value.trim()
        }
        if (fullName.isBlank()) {
            val nameMatch = nameRegex.find(fullText)
            if (nameMatch != null && !nameMatch.value.contains("Состояние", ignoreCase = true)) {
                fullName = nameMatch.value.trim()
            }
        }

        val dateMatch = Regex("""(?:на конец дня|конец дня)\s*[:]?\s*(\d{2}[.-]\d{2}[.-]\d{4})""", RegexOption.IGNORE_CASE).find(fullText)
        val accountDate = dateMatch?.groupValues?.get(1)?.trim() ?: ""

        val balanceMatch = Regex("""Баланс\s*:\s*([^\s;]+(?:\s*[рpР][уuУ][бbБ])?)""", RegexOption.IGNORE_CASE).find(fullText)
        val balance = balanceMatch?.groupValues?.get(1)?.trim() ?: "0.00"

        val debtMatch = Regex("""Основной долг\s*:\s*([^\s;]+(?:\s*[рpР][уuУ][бbБ])?)""", RegexOption.IGNORE_CASE).find(fullText)
        val mainDebt = debtMatch?.groupValues?.get(1)?.trim() ?: "0.00"

        val penaltyMatch = Regex("""Пеня[^\n\r:;]*:\s*([^\s;]+(?:\s*[рpР][уuУ][бbБ])?)""", RegexOption.IGNORE_CASE).find(fullText)
        val penalty = penaltyMatch?.groupValues?.get(1)?.trim() ?: "0.00"

        val groupMatch = Regex("""(?:Группа|Group)\s*[:]?\s*([^\s<;]+)""", RegexOption.IGNORE_CASE).find(fullText)
        val moodleGroup = groupMatch?.groupValues?.get(1)?.trim() ?: ""

        val loginMatch = Regex("""(?:Логин|Имя пользователя|Login)\s*[:]?\s*([^\s<;]+)""", RegexOption.IGNORE_CASE).find(fullText)
        val moodleLogin = loginMatch?.groupValues?.get(1)?.trim() ?: ""

        val passMatch = Regex("""(?:Пароль|Password)\s*[:]?\s*([^\s<;]+)""", RegexOption.IGNORE_CASE).find(fullText)
        val moodlePassword = passMatch?.groupValues?.get(1)?.trim() ?: ""

        val isDebtVal = (mainDebt.replace(",", ".").replace(" ", "").toDoubleOrNull() ?: 0.0) > 0.0 ||
                (penalty.replace(",", ".").replace(" ", "").toDoubleOrNull() ?: 0.0) > 0.0 ||
                (balance.replace(",", ".").replace(" ", "").toDoubleOrNull() ?: 0.0) < 0.0

        return StudentCabinetData(
            fullName = fullName,
            accountDate = accountDate,
            balance = balance,
            mainDebt = mainDebt,
            penalty = penalty,
            isDebt = isDebtVal,
            moodleGroup = moodleGroup,
            moodleLogin = moodleLogin,
            moodlePassword = moodlePassword
        )
    }
}
