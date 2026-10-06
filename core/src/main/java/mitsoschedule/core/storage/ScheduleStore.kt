package mitsoschedule.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mitsoschedule.core.model.DaySchedule
import mitsoschedule.core.model.StudentAuthCredentials
import mitsoschedule.core.model.StudentCabinetData
import mitsoschedule.core.model.UserSelection

/**
 * Локальное хранилище: выбранная группа, кэш расписания и сессия кабинета студента.
 *
 * Логин/пароль и кэш кабинета (в нём пароль Moodle) пишутся только в зашифрованном виде
 * через [cipher]. [keyPrefix] сохраняет совместимость с ключами, уже записанными на устройствах
 * (у телефона префикса нет, у часов он `watch_`).
 */
open class ScheduleStore(
    protected val dataStore: DataStore<Preferences>,
    keyPrefix: String,
    private val cipher: SecretCipher
) {
    protected val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val keySavedSelection = stringPreferencesKey("${keyPrefix}saved_user_selection")
    private val keyLastUpdateTime = stringPreferencesKey("${keyPrefix}last_update_timestamp")
    private val keyLastFetchMillis = longPreferencesKey("${keyPrefix}last_fetch_millis")
    private val keyCachedSchedule = stringPreferencesKey("${keyPrefix}cached_schedule_json")
    private val keyStudentCredentials = stringPreferencesKey("${keyPrefix}student_auth_credentials")
    private val keyStudentCabinetData = stringPreferencesKey("${keyPrefix}student_cached_cabinet_data")

    val savedSelectionFlow: Flow<UserSelection?> = dataStore.data.map { it[keySavedSelection].decode<UserSelection>() }

    val cachedScheduleFlow: Flow<List<DaySchedule>> =
        dataStore.data.map { it[keyCachedSchedule].decode<List<DaySchedule>>() ?: emptyList() }

    val lastUpdateFlow: Flow<String?> = dataStore.data.map { it[keyLastUpdateTime] }

    val lastFetchMillisFlow: Flow<Long> = dataStore.data.map { it[keyLastFetchMillis] ?: 0L }

    val studentCredentialsFlow: Flow<StudentAuthCredentials?> =
        dataStore.data.map { it[keyStudentCredentials].readSecret().decode<StudentAuthCredentials>() }

    val cachedStudentCabinetFlow: Flow<StudentCabinetData?> =
        dataStore.data.map { it[keyStudentCabinetData].readSecret().decode<StudentCabinetData>() }

    suspend fun saveSelection(selection: UserSelection) {
        dataStore.edit { it[keySavedSelection] = json.encodeToString(selection) }
    }

    suspend fun clearSelection() {
        dataStore.edit { it.remove(keySavedSelection) }
    }

    suspend fun saveSchedule(
        schedule: List<DaySchedule>,
        updateTime: String,
        fetchMillis: Long = System.currentTimeMillis()
    ) {
        dataStore.edit {
            it[keyCachedSchedule] = json.encodeToString(schedule)
            it[keyLastUpdateTime] = updateTime
            it[keyLastFetchMillis] = fetchMillis
        }
    }

    suspend fun clearScheduleCache() {
        dataStore.edit {
            it.remove(keyCachedSchedule)
            it.remove(keyLastUpdateTime)
            it.remove(keyLastFetchMillis)
        }
    }

    /** Если зашифровать не удалось, секрет не сохраняется (в открытом виде он на диск не попадает). */
    suspend fun saveStudentCredentials(credentials: StudentAuthCredentials) {
        val token = cipher.encrypt(json.encodeToString(credentials)) ?: return
        dataStore.edit { it[keyStudentCredentials] = token }
    }

    suspend fun saveStudentCabinetData(data: StudentCabinetData) {
        val token = cipher.encrypt(json.encodeToString(data)) ?: return
        dataStore.edit { it[keyStudentCabinetData] = token }
    }

    suspend fun clearStudentSession() {
        dataStore.edit {
            it.remove(keyStudentCredentials)
            it.remove(keyStudentCabinetData)
        }
    }

    /**
     * Шифрует значения, записанные старыми версиями приложения открытым текстом.
     * Если зашифровать не получилось, значение удаляется: лучше повторный вход, чем пароль на диске.
     */
    suspend fun migrateLegacySecrets() {
        dataStore.edit { prefs ->
            for (key in listOf(keyStudentCredentials, keyStudentCabinetData)) {
                val raw = prefs[key]
                if (raw.isNullOrBlank() || raw.startsWith(SecretCipher.PREFIX)) continue
                val token = cipher.encrypt(raw)
                if (token != null) prefs[key] = token else prefs.remove(key)
            }
        }
    }

    suspend fun getCachedSchedule(): List<DaySchedule> = cachedScheduleFlow.firstOrNull() ?: emptyList()

    suspend fun getSavedSelection(): UserSelection? = savedSelectionFlow.firstOrNull()

    suspend fun getLastUpdateTime(): String? = lastUpdateFlow.firstOrNull()

    suspend fun getLastFetchMillis(): Long = lastFetchMillisFlow.firstOrNull() ?: 0L

    /** Расшифровывает значение; устаревший открытый JSON читается как есть до миграции. */
    private fun String?.readSecret(): String? = when {
        isNullOrBlank() -> null
        startsWith(SecretCipher.PREFIX) -> cipher.decrypt(this)
        else -> this
    }

    private inline fun <reified T> String?.decode(): T? {
        if (isNullOrBlank()) return null
        return try {
            json.decodeFromString<T>(this)
        } catch (_: Exception) {
            null
        }
    }
}
