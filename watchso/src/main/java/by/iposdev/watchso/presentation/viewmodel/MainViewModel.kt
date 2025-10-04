package by.iposdev.watchso.presentation.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import by.iposdev.watchso.data.DaySchedule
import by.iposdev.watchso.data.Lesson
import by.iposdev.watchso.data.WebWorker
// Imports for UI model and parser function from presentation package
import by.iposdev.watchso.presentation.ParsedLessonEntry
import by.iposdev.watchso.presentation.UiDaySchedule
import by.iposdev.watchso.presentation.parseLessonEntryString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel(
    private val scheduleDataStore: DataStore<Preferences>,
    private val json: Json,
    private val webWorker: WebWorker
) : ViewModel() {

    private val tag = "MainViewModel"

    // Original schedule data
    private val _originalScheduleData = MutableStateFlow<List<DaySchedule>>(emptyList())
    // val scheduleData = mutableStateOf<List<DaySchedule>>(emptyList()) // Keep original for direct observation if needed by logic, but UI should use uiSchedule

    // Transformed schedule data for UI
    private val _uiSchedule = MutableStateFlow<List<UiDaySchedule>>(emptyList())
    val uiSchedule: StateFlow<List<UiDaySchedule>> = _uiSchedule.asStateFlow()

    val message = mutableStateOf("Инициализация...")
    val isLoading = mutableStateOf(false)

    private val _cacheInfoMessage = MutableStateFlow<String?>(null)
    val cacheInfoMessage: StateFlow<String?> = _cacheInfoMessage.asStateFlow()

    companion object {
        private val KEY_SCHEDULE_JSON = stringPreferencesKey("schedule_json")
        private val KEY_SCHEDULE_MONDAY_DATE_STRING = stringPreferencesKey("schedule_monday_date_string")
        private val KEY_CACHE_TIMESTAMP = longPreferencesKey("cache_timestamp")
        private val KEY_CACHE_IS_TEST_DATA = booleanPreferencesKey("cache_is_test_data")
    }

    init {
        Log.d(tag, "ViewModel initialized. Loading schedule...")
        loadSchedule()
    }

    private suspend fun processAndSetSchedule(newSchedule: List<DaySchedule>, sourceMessage: String, cacheType: String?, timestamp: Long?) {
        _originalScheduleData.value = newSchedule
        transformAndSetUiSchedule(newSchedule) // Perform transformation
        message.value = sourceMessage
        if (timestamp != null && cacheType != null) {
            _cacheInfoMessage.value = "Кэш ($cacheType): ${formatTimestamp(timestamp)}"
        } else if (cacheType != null) { // For cases like "Кеш не найден" or "Обновление..."
            _cacheInfoMessage.value = cacheType
        }
        Log.i(tag, "Schedule processed. Info: ${_cacheInfoMessage.value}")
        isLoading.value = false
    }

    private suspend fun transformAndSetUiSchedule(sourceSchedule: List<DaySchedule>) {
        if (sourceSchedule.isEmpty()) {
            _uiSchedule.value = emptyList()
            return
        }
        // Perform heavy transformation in a background thread
        val transformed = withContext(Dispatchers.Default) {
            val calendar = Calendar.getInstance()
            val dayFormat = SimpleDateFormat("EEEE", Locale("ru"))
            var currentDayName = dayFormat.format(calendar.time)
            currentDayName = currentDayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            val mappedSchedule = sourceSchedule.map { dayScheduleData ->
                val parsedLessonsForDay = mutableListOf<ParsedLessonEntry>()
                dayScheduleData.lessons.forEach { lesson ->
                    val lessonTextContent = lesson.text
                    val timeSplitRegex = Regex("(\\d{2}\\.\\d{2}-\\d{2}\\.\\d{2})")
                    val entries = mutableListOf<String>()
                    var lastEnd = 0
                    timeSplitRegex.findAll(lessonTextContent).forEach { matchResult ->
                        if (matchResult.range.first > lastEnd) {
                            val textBeforeMatch = lessonTextContent.substring(lastEnd, matchResult.range.first).trim()
                            if(textBeforeMatch.isNotBlank()) entries.add(textBeforeMatch)
                        }
                        val nextMatchStart = timeSplitRegex.find(lessonTextContent, matchResult.range.last + 1)?.range?.first
                        val endOfCurrentEntry = nextMatchStart ?: lessonTextContent.length
                        entries.add(lessonTextContent.substring(matchResult.range.first, endOfCurrentEntry).trim())
                        lastEnd = endOfCurrentEntry
                    }
                    if (entries.isEmpty() && lessonTextContent.isNotBlank()) entries.add(lessonTextContent.trim())
                    entries.filter { it.isNotBlank() }.forEach { entryString ->
                        val parsed = parseLessonEntryString(entryString) // This function is from presentation package
                        if (parsed.subject?.isNotBlank() == true &&
                            parsed.subject != "(нет занятий)" &&
                            parsed.subject != "(описание отсутствует)") {
                            parsedLessonsForDay.add(parsed)
                        }
                    }
                }
                UiDaySchedule(dayScheduleData.dayTitle, parsedLessonsForDay)
            }.filter { it.lessons.isNotEmpty() }

            val startIndex = mappedSchedule.indexOfFirst { it.dayTitle.startsWith(currentDayName, ignoreCase = true) }
            if (startIndex != -1) mappedSchedule.subList(startIndex, mappedSchedule.size) else mappedSchedule
        }
        _uiSchedule.value = transformed
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun getCurrentMondayDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val sdf = SimpleDateFormat("EEEE, d MMMM", Locale("ru"))
        return sdf.format(calendar.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    private fun extractRelevantMondayDateString(schedule: List<DaySchedule>): String? {
        // ... (implementation remains the same)
        if (schedule.isEmpty()) return null
        val firstDayTitle = schedule.first().dayTitle
        val datePartRegex = "(\\d{1,2}\\s+\\p{L}+)".toRegex(RegexOption.IGNORE_CASE)
        val dayAndMonthMatch = datePartRegex.find(firstDayTitle)
        val dayAndMonth = dayAndMonthMatch?.groupValues?.getOrNull(1) ?: return null
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val dateStringWithYear = "$dayAndMonth $currentYear"
        val inputSdf = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
        try {
            val date = inputSdf.parse(dateStringWithYear)
            if (date != null) {
                val calendar = Calendar.getInstance()
                calendar.time = date
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val outputSdf = SimpleDateFormat("EEEE, d MMMM", Locale("ru"))
                return outputSdf.format(calendar.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing date from schedule title: $firstDayTitle", e)
        }
        return null
    }

    fun loadSchedule(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            isLoading.value = true
            if (!forceRefresh) {
                message.value = "Проверка кэша..."
                Log.d(tag, "Checking cache...")
                val preferences = scheduleDataStore.data.firstOrNull()
                val cachedScheduleJson = preferences?.get(KEY_SCHEDULE_JSON)
                val cachedMondayDateString = preferences?.get(KEY_SCHEDULE_MONDAY_DATE_STRING)
                val currentMondayDateString = getCurrentMondayDateString()

                if (cachedScheduleJson != null && cachedMondayDateString == currentMondayDateString) {
                    Log.d(tag, "Cache is valid for the current week: $currentMondayDateString.")
                    try {
                        val deserializedSchedule = json.decodeFromString<List<DaySchedule>>(cachedScheduleJson)
                        if (deserializedSchedule.isNotEmpty()) {
                            val timestamp = preferences.get(KEY_CACHE_TIMESTAMP)
                            val isTestData = preferences.get(KEY_CACHE_IS_TEST_DATA) ?: false
                            val dataType = if (isTestData) "Тест" else "Реал"
                            processAndSetSchedule(deserializedSchedule, "Расписание загружено из кэша.", dataType, timestamp)
                            return@launch
                        } else {
                            Log.w(tag, "Cached schedule is empty. Fetching from network.")
                            message.value = "Кэш пуст. Загрузка из сети..."
                            _cacheInfoMessage.value = "Кеш не используется (пуст)"
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error deserializing schedule from cache. Fetching from network.", e)
                        message.value = "Ошибка кэша. Загрузка из сети..."
                        _cacheInfoMessage.value = "Кеш не используется (ошибка)"
                        scheduleDataStore.edit { prefs ->
                            prefs.remove(KEY_SCHEDULE_JSON)
                            prefs.remove(KEY_SCHEDULE_MONDAY_DATE_STRING)
                            prefs.remove(KEY_CACHE_TIMESTAMP)
                            prefs.remove(KEY_CACHE_IS_TEST_DATA)
                        }
                    }
                } else { /* Cache not valid or not found */ }
            } else { /* Force refresh */ }
            // Fall through to fetch from network if cache is not used or forceRefresh is true
            if (forceRefresh) {
                 message.value = "Принудительное обновление из сети..."
                 _cacheInfoMessage.value = "Обновление..."
                 Log.d(tag, "Forcing refresh from network.")
            } else if (scheduleDataStore.data.firstOrNull()?.get(KEY_SCHEDULE_JSON) == null) {
                 Log.d(tag, "Cache not found.")
                 message.value = "Кэш не найден. Загрузка из сети..."
                 _cacheInfoMessage.value = "Кеш не найден"
            } else {
                 Log.d(tag, "Cache is outdated. Fetching from network.")
                 message.value = "Кэш устарел. Загрузка из сети..."
                 _cacheInfoMessage.value = "Кеш устарел"
            }
            fetchScheduleFromNetwork()
        }
    }

    private suspend fun fetchScheduleFromNetwork() {
        Log.d(tag, "fetchScheduleFromNetwork called.")
        isLoading.value = true
        // _originalScheduleData.value = emptyList() // Clear only if network call is initiated, not on fallback
        // _uiSchedule.value = emptyList()

        var dataFromWebWorker: List<DaySchedule> = emptyList()
        var webError: Exception? = null

        try {
            Log.d(tag, "Calling webWorker.sendRequest()")
            dataFromWebWorker = webWorker.sendRequest()
            Log.d(tag, "Data received from WebWorker, size: ${dataFromWebWorker.size}")
        } catch (e: Exception) {
            Log.e(tag, "Error fetching schedule from WebWorker", e)
            webError = e
        }

        val currentTimestamp = System.currentTimeMillis()

        if (dataFromWebWorker.isNotEmpty()) {
            val scheduleMondayDateString = extractRelevantMondayDateString(dataFromWebWorker)
            if (scheduleMondayDateString != null) {
                try {
                    val scheduleJsonToCache = json.encodeToString(dataFromWebWorker)
                    scheduleDataStore.edit { preferences ->
                        preferences[KEY_SCHEDULE_JSON] = scheduleJsonToCache
                        preferences[KEY_SCHEDULE_MONDAY_DATE_STRING] = scheduleMondayDateString
                        preferences[KEY_CACHE_TIMESTAMP] = currentTimestamp
                        preferences[KEY_CACHE_IS_TEST_DATA] = false
                    }
                    processAndSetSchedule(dataFromWebWorker, "Расписание обновлено из сети.", "Реал", currentTimestamp)
                } catch (e: Exception) {
                    Log.e(tag, "Error serializing and saving schedule to DataStore", e)
                    // Still show network data, but indicate caching error
                    processAndSetSchedule(dataFromWebWorker, "Расписание загружено, но не сохранено в кэш.", "Данные из сети (ошибка кеширования)", null)
                }
            } else {
                 processAndSetSchedule(dataFromWebWorker, "Расписание загружено, но не удалось определить дату для кэширования.", "Данные из сети (ошибка ключа кеша)", null)
                Log.w(tag, "Schedule fetched, but could not determine Monday date string for caching.")
            }
        } else { // Network error or empty data
            Log.w(tag, "WebWorker returned an empty list or an error occurred.")
            val preferences = scheduleDataStore.data.firstOrNull()
            val cachedScheduleJson = preferences?.get(KEY_SCHEDULE_JSON)
            var loadedFromOldCache = false
            if (cachedScheduleJson != null) {
                try {
                    val deserializedSchedule = json.decodeFromString<List<DaySchedule>>(cachedScheduleJson)
                    if (deserializedSchedule.isNotEmpty()) {
                        val timestamp = preferences.get(KEY_CACHE_TIMESTAMP)
                        val isTestData = preferences.get(KEY_CACHE_IS_TEST_DATA) ?: false
                        val dataType = if (isTestData) "Тест (устар.)" else "Реал (устар.)"
                        val errMsg = if (webError != null) "Ошибка сети. Показаны ранее загруженные данные." else "Сайт недоступен. Показаны ранее загруженные данные."
                        processAndSetSchedule(deserializedSchedule, errMsg, dataType, timestamp)
                        loadedFromOldCache = true
                    }
                } catch (e: Exception) { Log.w(tag, "Network error, and also failed to reload from cache.", e) }
            }

            if (!loadedFromOldCache) {
                val testData = getTestScheduleData()
                val currentWeekMondayString = getCurrentMondayDateString()
                val errMsg = when { /* ... error messages ... */ 
                    webError != null && cachedScheduleJson == null -> "Ошибка сети. Кэш пуст. Тестовые данные."
                    webError != null -> "Ошибка сети. Кэш не прочитан. Тестовые данные."
                    cachedScheduleJson == null -> "Сайт недоступен. Кэш пуст. Тестовые данные."
                    else -> "Сайт недоступен. Кэш не прочитан. Тестовые данные."
                }
                try {
                    val testDataJson = json.encodeToString(testData)
                    scheduleDataStore.edit { prefs ->
                        prefs[KEY_SCHEDULE_JSON] = testDataJson
                        prefs[KEY_SCHEDULE_MONDAY_DATE_STRING] = currentWeekMondayString
                        prefs[KEY_CACHE_TIMESTAMP] = currentTimestamp
                        prefs[KEY_CACHE_IS_TEST_DATA] = true
                    }
                     processAndSetSchedule(testData, errMsg, "Тест", currentTimestamp)
                    Log.i(tag, "Test data cached. Info: ${_cacheInfoMessage.value}")
                } catch (e: Exception) {
                    Log.e(tag, "Error serializing and saving test data to DataStore", e)
                    processAndSetSchedule(testData, errMsg, "Тест. данные (ошибка кеширования)", null)
                }
            }
        }
        isLoading.value = false
        Log.d(tag, "fetchScheduleFromNetwork completed. isLoading: ${isLoading.value}, message: '${message.value}', cacheInfo: '${_cacheInfoMessage.value}'")
    }

    fun fetchSchedule() {
        loadSchedule(forceRefresh = true)
    }

    private fun getTestScheduleData(): List<DaySchedule> {
        Log.i(tag, "Providing test schedule data.")
        // ... (implementation remains the same)
        return listOf(
            DaySchedule("Понедельник (Тест)", listOf(Lesson("08.00-09.25 Тестовый Предмет 1 (каб. 101) 09.35-11.00 Тестовый Предмет 2 (каб. 102) 11.10-12.35 (нет занятий)"))),
            DaySchedule("Вторник (Тест)", listOf(Lesson("09.35-11.00 Важная Лекция (Зал А) 11.10-12.35 Практика по кодингу (комп. класс)"))),
            DaySchedule("Среда (Тест)", listOf(Lesson("08.00-09.25 (нет занятий) 09.35-11.00 Еще один предмет (ауд. 303)"))),
            DaySchedule("Четверг (Тест)", listOf(Lesson("14.40-16.05 Очень длинное название дисциплины для проверки отображения и переноса строк (каб. 505)"))),
            DaySchedule("Пятница (Тест)", listOf(Lesson("09.35-11.00 Консультация (каб. деканат) 11.10-12.35 Свободное время")))
        )
    }
}
