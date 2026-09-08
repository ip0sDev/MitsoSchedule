package by.iposdev.watchso.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate

val Context.watchDataStore: DataStore<Preferences> by preferencesDataStore(name = "mitso_watch_prefs")

class WatchPreferencesManager(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        private val KEY_SAVED_SELECTION = stringPreferencesKey("watch_saved_user_selection")
        private val KEY_LAST_UPDATE_TIME = stringPreferencesKey("watch_last_update_timestamp")
        private val KEY_LAST_FETCH_MILLIS = longPreferencesKey("watch_last_fetch_millis")
        private val KEY_CACHED_SCHEDULE = stringPreferencesKey("watch_cached_schedule_json")
        private val KEY_STUDENT_CREDENTIALS = stringPreferencesKey("watch_student_auth_credentials")
        private val KEY_STUDENT_CABINET_DATA = stringPreferencesKey("watch_student_cached_cabinet_data")

        fun shouldAutoRefresh(lastFetchMillis: Long): Boolean {
            if (lastFetchMillis <= 0L) return true

            val now = System.currentTimeMillis()
            val elapsedMillis = now - lastFetchMillis
            if (elapsedMillis < 0) return true

            val dayOfWeek = LocalDate.now().dayOfWeek
            val isFridayOrLater = dayOfWeek == DayOfWeek.FRIDAY ||
                    dayOfWeek == DayOfWeek.SATURDAY ||
                    dayOfWeek == DayOfWeek.SUNDAY

            return if (isFridayOrLater) {
                // С пятницы и далее: пытаемся обновлять каждый день (раз в 24 часа)
                val oneDayMillis = 24 * 60 * 60 * 1000L
                elapsedMillis >= oneDayMillis
            } else {
                // Понедельник - четверг: не обновляем автоматически, если кэш младше 7 дней (недели)
                val oneWeekMillis = 7 * 24 * 60 * 60 * 1000L
                elapsedMillis >= oneWeekMillis
            }
        }
    }

    val savedSelectionFlow: Flow<UserSelection?> = context.watchDataStore.data.map { preferences ->
        val raw = preferences[KEY_SAVED_SELECTION]
        if (!raw.isNullOrBlank()) {
            try {
                json.decodeFromString<UserSelection>(raw)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    val cachedScheduleFlow: Flow<List<DaySchedule>> = context.watchDataStore.data.map { preferences ->
        val raw = preferences[KEY_CACHED_SCHEDULE]
        if (!raw.isNullOrBlank()) {
            try {
                json.decodeFromString<List<DaySchedule>>(raw)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    val lastUpdateFlow: Flow<String?> = context.watchDataStore.data.map { preferences ->
        preferences[KEY_LAST_UPDATE_TIME]
    }

    val lastFetchMillisFlow: Flow<Long> = context.watchDataStore.data.map { preferences ->
        preferences[KEY_LAST_FETCH_MILLIS] ?: 0L
    }

    val studentCredentialsFlow: Flow<StudentAuthCredentials?> = context.watchDataStore.data.map { preferences ->
        val raw = preferences[KEY_STUDENT_CREDENTIALS]
        if (!raw.isNullOrBlank()) {
            try {
                json.decodeFromString<StudentAuthCredentials>(raw)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    val cachedStudentCabinetFlow: Flow<StudentCabinetData?> = context.watchDataStore.data.map { preferences ->
        val raw = preferences[KEY_STUDENT_CABINET_DATA]
        if (!raw.isNullOrBlank()) {
            try {
                json.decodeFromString<StudentCabinetData>(raw)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    suspend fun saveSelection(selection: UserSelection) {
        context.watchDataStore.edit { preferences ->
            preferences[KEY_SAVED_SELECTION] = json.encodeToString(selection)
        }
    }

    suspend fun saveSchedule(schedule: List<DaySchedule>, updateTime: String, fetchMillis: Long = System.currentTimeMillis()) {
        context.watchDataStore.edit { preferences ->
            preferences[KEY_CACHED_SCHEDULE] = json.encodeToString(schedule)
            preferences[KEY_LAST_UPDATE_TIME] = updateTime
            preferences[KEY_LAST_FETCH_MILLIS] = fetchMillis
        }
    }

    suspend fun saveStudentCredentials(credentials: StudentAuthCredentials) {
        context.watchDataStore.edit { preferences ->
            preferences[KEY_STUDENT_CREDENTIALS] = json.encodeToString(credentials)
        }
    }

    suspend fun saveStudentCabinetData(data: StudentCabinetData) {
        context.watchDataStore.edit { preferences ->
            preferences[KEY_STUDENT_CABINET_DATA] = json.encodeToString(data)
        }
    }

    suspend fun clearStudentSession() {
        context.watchDataStore.edit { preferences ->
            preferences.remove(KEY_STUDENT_CREDENTIALS)
            preferences.remove(KEY_STUDENT_CABINET_DATA)
        }
    }
}
