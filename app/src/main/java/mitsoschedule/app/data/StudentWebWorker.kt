package mitsoschedule.app.data

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
import java.util.concurrent.TimeUnit

class StudentWebWorker(
    private var baseUrl: String = PreferencesManager.DEFAULT_SERVER_URL
) {

    private val TAG = "StudentWebWorker"
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

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
                    val responseText = response.body?.string().orEmpty()

                    if (response.code == 401) {
                        return@withContext Result.failure(Exception("Неверный номер лицевого счета или пароль"))
                    }

                    if (response.code == 429) {
                        val errorMsg = try {
                            json.parseToJsonElement(responseText).jsonObject["error"]?.jsonPrimitive?.content
                        } catch (e: Exception) { null }
                        return@withContext Result.failure(
                            Exception(errorMsg ?: "Превышен лимит запросов. Пожалуйста, подождите немного.")
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
