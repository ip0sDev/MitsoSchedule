package mitsoschedule.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mitso_schedule_prefs")

class PreferencesManager(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        const val DEFAULT_SERVER_URL = "https://university.visorlink.org"
        const val ONE_WEEK_MILLIS = 7 * 24 * 60 * 60 * 1000L
        private val KEY_SERVER_URL = stringPreferencesKey("server_base_url")
        private val KEY_SAVED_SELECTION = stringPreferencesKey("saved_user_selection")
        private val KEY_LAST_UPDATE_TIME = stringPreferencesKey("last_update_timestamp")
        private val KEY_LAST_FETCH_MILLIS = longPreferencesKey("last_fetch_millis")
        private val KEY_CACHED_SCHEDULE = stringPreferencesKey("cached_schedule_json")
        private val KEY_STUDENT_CREDENTIALS = stringPreferencesKey("student_auth_credentials")
        private val KEY_STUDENT_CABINET_DATA = stringPreferencesKey("student_cached_cabinet_data")
        private val KEY_THEME_MODE = stringPreferencesKey("app_theme_mode")

        fun isScheduleOlderThanWeek(
            lastFetchMillis: Long,
            schedules: List<DaySchedule> = emptyList(),
            today: LocalDate = LocalDate.now()
        ): Boolean {
            if (lastFetchMillis <= 0L) return true
            val now = System.currentTimeMillis()
            val elapsedMillis = now - lastFetchMillis
            if (elapsedMillis < 0L || elapsedMillis >= ONE_WEEK_MILLIS) return true
            if (schedules.isEmpty()) return true

            var hasValidDate = false
            for (day in schedules) {
                val parsed = parseDateFromDaySchedule(day, today)
                if (parsed != null) {
                    hasValidDate = true
                    if (!parsed.isBefore(today)) {
                        return false
                    }
                }
            }
            return hasValidDate
        }

        fun parseDateText(text: String, today: LocalDate = LocalDate.now()): LocalDate? {
            val trimmed = text.trim()
            val parts = trimmed.split("\\s+".toRegex())
            if (parts.size >= 2) {
                val dayNum = parts[0].toIntOrNull()
                val monthName = parts[1].lowercase()
                val monthNum = when {
                    monthName.startsWith("янв") -> 1
                    monthName.startsWith("фев") -> 2
                    monthName.startsWith("мар") -> 3
                    monthName.startsWith("апр") -> 4
                    monthName.startsWith("май") || monthName.startsWith("мая") -> 5
                    monthName.startsWith("июн") -> 6
                    monthName.startsWith("июл") -> 7
                    monthName.startsWith("авг") -> 8
                    monthName.startsWith("сен") -> 9
                    monthName.startsWith("окт") -> 10
                    monthName.startsWith("ноя") -> 11
                    monthName.startsWith("дек") -> 12
                    else -> 0
                }
                if (dayNum != null && monthNum in 1..12) {
                    val currentYear = today.year
                    val inferredYear = if (today.monthValue == 1 && monthNum == 12) {
                        currentYear - 1
                    } else if (today.monthValue == 12 && monthNum == 1) {
                        currentYear + 1
                    } else {
                        currentYear
                    }
                    return try {
                        LocalDate.of(inferredYear, monthNum, dayNum)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            return null
        }

        fun parseWeekDateRange(weekName: String, today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate>? {
            val parts = weekName.split("[-–—]".toRegex())
            if (parts.size == 2) {
                val start = parseDateText(parts[0], today)
                val end = parseDateText(parts[1], today)
                if (start != null && end != null) {
                    return Pair(start, end)
                }
            }
            return null
        }

        fun parseDateFromDaySchedule(day: DaySchedule, today: LocalDate = LocalDate.now()): LocalDate? {
            val subtitle = day.dateSubtitle?.trim().orEmpty()
            val fullTitle = day.dayTitle.trim()
            val dateText = if (subtitle.isNotBlank()) subtitle else fullTitle.substringAfter(",").trim()
            return parseDateText(dateText, today)
        }

        fun findCurrentWeekId(
            weeks: List<OptionItem>,
            schedules: List<DaySchedule>,
            today: LocalDate = LocalDate.now()
        ): String? {
            // 1. Try finding a DaySchedule that matches today
            val todaySchedule = schedules.find { day ->
                val parsed = parseDateFromDaySchedule(day, today)
                parsed != null && parsed.isEqual(today)
            }
            if (todaySchedule != null && todaySchedule.weekId.isNotBlank() && todaySchedule.weekId != "0" && todaySchedule.weekId != "ALL") {
                return todaySchedule.weekId
            }

            // 2. Try finding a DaySchedule within current week (Monday..Sunday)
            val startOfWeek = today.with(DayOfWeek.MONDAY)
            val endOfWeek = today.with(DayOfWeek.SUNDAY)
            val thisWeekSchedule = schedules.find { day ->
                val parsed = parseDateFromDaySchedule(day, today)
                parsed != null && !parsed.isBefore(startOfWeek) && !parsed.isAfter(endOfWeek)
            }
            if (thisWeekSchedule != null && thisWeekSchedule.weekId.isNotBlank() && thisWeekSchedule.weekId != "0" && thisWeekSchedule.weekId != "ALL") {
                return thisWeekSchedule.weekId
            }

            // 3. Check for explicitly designated "Текущая неделя" in weeks list
            val currentNamedWeek = weeks.find { it.id != "ALL" && it.name.contains("текущ", ignoreCase = true) }
            if (currentNamedWeek != null) {
                return currentNamedWeek.id
            }

            // 4. Try parsing week date ranges from week names (e.g. "21 сентября - 27 сентября")
            for (w in weeks) {
                if (w.id == "ALL") continue
                val range = parseWeekDateRange(w.name, today)
                if (range != null && !today.isBefore(range.first) && !today.isAfter(range.second)) {
                    return w.id
                }
            }

            // 5. Find closest week from schedules
            var closestWeekId: String? = null
            var minDiffDays = Long.MAX_VALUE
            for (day in schedules) {
                val parsed = parseDateFromDaySchedule(day, today) ?: continue
                val diff = kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(today, parsed))
                if (diff < minDiffDays && day.weekId.isNotBlank() && day.weekId != "0" && day.weekId != "ALL") {
                    minDiffDays = diff
                    closestWeekId = day.weekId
                }
            }
            if (closestWeekId != null) return closestWeekId

            val realWeeks = weeks.filter { it.id != "ALL" }
            return realWeeks.firstOrNull()?.id ?: realWeeks.lastOrNull()?.id
        }

        fun shouldAutoRefresh(lastFetchMillis: Long): Boolean {
            if (lastFetchMillis <= 0L) return true

            val now = System.currentTimeMillis()
            val elapsedMillis = now - lastFetchMillis
            if (elapsedMillis < 0) return true

            // Если старше 7 дней (недели) — ВСЕГДА обновляем
            if (elapsedMillis >= ONE_WEEK_MILLIS) return true

            val dayOfWeek = LocalDate.now().dayOfWeek
            val isFridayOrLater = dayOfWeek == DayOfWeek.FRIDAY ||
                    dayOfWeek == DayOfWeek.SATURDAY ||
                    dayOfWeek == DayOfWeek.SUNDAY

            return if (isFridayOrLater) {
                // С пятницы и далее: пытаемся обновлять каждый день (раз в 24 часа)
                val oneDayMillis = 24 * 60 * 60 * 1000L
                elapsedMillis >= oneDayMillis
            } else {
                false
            }
        }
    }

    val savedSelectionFlow: Flow<UserSelection?> = context.dataStore.data.map { preferences ->
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

    val cachedScheduleFlow: Flow<List<DaySchedule>> = context.dataStore.data.map { preferences ->
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

    val lastUpdateFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_UPDATE_TIME]
    }

    val lastFetchMillisFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_FETCH_MILLIS] ?: 0L
    }

    val studentCredentialsFlow: Flow<StudentAuthCredentials?> = context.dataStore.data.map { preferences ->
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

    val cachedStudentCabinetFlow: Flow<StudentCabinetData?> = context.dataStore.data.map { preferences ->
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

    val serverUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SERVER_URL]?.takeIf { it.isNotBlank() } ?: DEFAULT_SERVER_URL
    }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SERVER_URL] = url.trimEnd('/')
        }
    }

    suspend fun clearScheduleCache() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_CACHED_SCHEDULE)
            preferences.remove(KEY_LAST_UPDATE_TIME)
            preferences.remove(KEY_LAST_FETCH_MILLIS)
        }
    }

    suspend fun saveSelection(selection: UserSelection) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SAVED_SELECTION] = json.encodeToString(selection)
        }
    }

    suspend fun saveSchedule(schedule: List<DaySchedule>, updateTime: String, fetchMillis: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CACHED_SCHEDULE] = json.encodeToString(schedule)
            preferences[KEY_LAST_UPDATE_TIME] = updateTime
            preferences[KEY_LAST_FETCH_MILLIS] = fetchMillis
        }
    }

    suspend fun saveLastUpdateTime(timeString: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_UPDATE_TIME] = timeString
        }
    }

    suspend fun clearSelection() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_SAVED_SELECTION)
        }
    }

    suspend fun saveStudentCredentials(credentials: StudentAuthCredentials) {
        context.dataStore.edit { preferences ->
            preferences[KEY_STUDENT_CREDENTIALS] = json.encodeToString(credentials)
        }
    }

    suspend fun saveStudentCabinetData(data: StudentCabinetData) {
        context.dataStore.edit { preferences ->
            preferences[KEY_STUDENT_CABINET_DATA] = json.encodeToString(data)
        }
    }

    suspend fun clearStudentSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_STUDENT_CREDENTIALS)
            preferences.remove(KEY_STUDENT_CABINET_DATA)
        }
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "system"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }

    suspend fun getLastFetchMillis(): Long = lastFetchMillisFlow.firstOrNull() ?: 0L
}
