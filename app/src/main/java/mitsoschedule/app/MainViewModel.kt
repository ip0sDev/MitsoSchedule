package mitsoschedule.app

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import mitsoschedule.app.data.DaySchedule
import mitsoschedule.app.data.OptionItem
import mitsoschedule.app.data.PreferencesManager
import mitsoschedule.app.data.ServerHealth
import mitsoschedule.app.data.StudentAuthCredentials
import mitsoschedule.app.data.StudentCabinetData
import mitsoschedule.app.data.StudentWebWorker
import mitsoschedule.app.data.UserSelection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val ALL_WEEKS_ID = "ALL"
        val ALL_WEEKS_OPTION = OptionItem(id = ALL_WEEKS_ID, name = "Все доступные недели")
    }

    private val webWorker = WebWorker()
    private val studentWebWorker = StudentWebWorker()
    private val preferencesManager = PreferencesManager(application.applicationContext)

    // Navigation state: 0 -> Schedule, 1 -> Cabinet
    private val _currentTab = mutableIntStateOf(0)
    val currentTab: State<Int> = _currentTab

    fun selectTab(tabIndex: Int) {
        _currentTab.intValue = tabIndex
    }

    // Schedule UI States
    private val _userSelection = mutableStateOf(UserSelection())
    val userSelection: State<UserSelection> = _userSelection

    private val _scheduleData = mutableStateOf<List<DaySchedule>>(emptyList())
    val scheduleData: State<List<DaySchedule>> = _scheduleData

    private val _faculties = mutableStateOf<List<OptionItem>>(emptyList())
    val faculties: State<List<OptionItem>> = _faculties

    private val _forms = mutableStateOf<List<OptionItem>>(emptyList())
    val forms: State<List<OptionItem>> = _forms

    private val _courses = mutableStateOf<List<OptionItem>>(emptyList())
    val courses: State<List<OptionItem>> = _courses

    private val _groups = mutableStateOf<List<OptionItem>>(emptyList())
    val groups: State<List<OptionItem>> = _groups

    private val _weeks = mutableStateOf<List<OptionItem>>(emptyList())
    val weeks: State<List<OptionItem>> = _weeks

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _isLoadingOptions = mutableStateOf(false)
    val isLoadingOptions: State<Boolean> = _isLoadingOptions

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _lastUpdateTime = mutableStateOf<String?>(null)
    val lastUpdateTime: State<String?> = _lastUpdateTime

    // Student Cabinet UI States
    private val _studentCabinetData = mutableStateOf<StudentCabinetData?>(null)
    val studentCabinetData: State<StudentCabinetData?> = _studentCabinetData

    private val _isStudentLoading = mutableStateOf(false)
    val isStudentLoading: State<Boolean> = _isStudentLoading

    private val _studentErrorMessage = mutableStateOf<String?>(null)
    val studentErrorMessage: State<String?> = _studentErrorMessage

    // Server & Settings States
    private val _serverHealth = mutableStateOf<ServerHealth?>(null)
    val serverHealth: State<ServerHealth?> = _serverHealth

    private val _isCheckingHealth = mutableStateOf(false)
    val isCheckingHealth: State<Boolean> = _isCheckingHealth

    private val _serverUrl = mutableStateOf(PreferencesManager.DEFAULT_SERVER_URL)
    val serverUrl: State<String> = _serverUrl

    private val _themeMode = mutableStateOf("system")
    val themeMode: State<String> = _themeMode

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    private var savedCredentials: StudentAuthCredentials? = null

    init {
        loadInitialData()
        loadInitialStudentData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // 0. Load server URL and theme from preferences
            val savedServerUrl = preferencesManager.serverUrlFlow.firstOrNull() ?: PreferencesManager.DEFAULT_SERVER_URL
            _serverUrl.value = savedServerUrl
            webWorker.updateBaseUrl(savedServerUrl)
            studentWebWorker.updateBaseUrl(savedServerUrl)
            checkServerHealth()

            val savedTheme = preferencesManager.themeModeFlow.firstOrNull() ?: "system"
            _themeMode.value = savedTheme

            // 1. Read local preferences immediately
            val saved = preferencesManager.savedSelectionFlow.firstOrNull()
            val savedTime = preferencesManager.lastUpdateFlow.firstOrNull()
            val cachedSchedule = preferencesManager.cachedScheduleFlow.firstOrNull()
            val lastFetchMillis = preferencesManager.lastFetchMillisFlow.firstOrNull() ?: 0L

            if (savedTime != null) {
                _lastUpdateTime.value = savedTime
            }

            // 2. Immediately apply saved selection and cached schedule to UI (0ms delay)
            if (saved != null) {
                _userSelection.value = saved
            }

            if (!cachedSchedule.isNullOrEmpty()) {
                _allScheduleData.value = cachedSchedule

                // Populate weeks list from cached schedule immediately so week navigation works offline
                val cachedWeeks = cachedSchedule
                    .map { OptionItem(it.weekId, it.weekName) }
                    .filter { it.id.isNotBlank() }
                    .distinctBy { it.id }
                if (cachedWeeks.isNotEmpty()) {
                    _weeks.value = processWeekList(cachedWeeks)
                }

                // If saved week is invalid or default "0" or blank, resolve to current week
                if (saved != null) {
                    val knownWeekIds = cachedSchedule.map { it.weekId }.filter { it.isNotBlank() }.distinct()
                    val savedWeekId = saved.weekId
                    if (savedWeekId != ALL_WEEKS_ID && (savedWeekId.isBlank() || savedWeekId == "0" || savedWeekId !in knownWeekIds)) {
                        val currentWeekId = PreferencesManager.findCurrentWeekId(_weeks.value, cachedSchedule)
                        if (currentWeekId != null) {
                            val currentWeekName = _weeks.value.find { it.id == currentWeekId }?.name
                                ?: cachedSchedule.firstOrNull { it.weekId == currentWeekId }?.weekName ?: saved.weekName
                            _userSelection.value = saved.copy(weekId = currentWeekId, weekName = currentWeekName)
                        }
                    }
                }

                // Filter and display schedule instantly on screen
                filterAndApplyDisplayedSchedule()
            }

            // 3. Load options and trigger background refresh asynchronously without blocking cached UI
            launch {
                loadFaculties()
            }

            if (saved != null && saved.isComplete) {
                launch {
                    try {
                        loadDependentOptionsForSelection(saved)
                    } catch (e: Exception) {
                        // ignore network error for offline cache
                    }
                }

                val isStale = PreferencesManager.isScheduleOlderThanWeek(lastFetchMillis, cachedSchedule ?: emptyList())
                val shouldRefresh = PreferencesManager.shouldAutoRefresh(lastFetchMillis)
                if (isStale || shouldRefresh || cachedSchedule.isNullOrEmpty()) {
                    fetchSchedule(isManualRefresh = isStale)
                }
            }
        }
    }

    fun syncIfOlderThanWeek(force: Boolean = false) {
        val current = _userSelection.value
        if (!current.isComplete) return
        if (_isLoading.value) return

        viewModelScope.launch {
            val lastFetch = preferencesManager.getLastFetchMillis()
            val cached = _allScheduleData.value.ifEmpty {
                preferencesManager.cachedScheduleFlow.firstOrNull() ?: emptyList()
            }
            if (force || PreferencesManager.isScheduleOlderThanWeek(lastFetch, cached)) {
                fetchSchedule(isManualRefresh = true)
            }
        }
    }

    private fun loadInitialStudentData() {
        viewModelScope.launch {
            // Load cached cabinet data if any
            val cachedData = preferencesManager.cachedStudentCabinetFlow.firstOrNull()
            if (cachedData != null) {
                _studentCabinetData.value = cachedData
            }

            // Load credentials and auto-refresh
            val credentials = preferencesManager.studentCredentialsFlow.firstOrNull()
            if (credentials != null && credentials.rememberMe && credentials.login.isNotBlank()) {
                savedCredentials = credentials
                refreshStudentCabinet()
            }
        }
    }

    // ----------------- Schedule Methods -----------------

    fun loadFaculties() {
        viewModelScope.launch {
            _isLoadingOptions.value = true
            try {
                val (_, facultyList) = webWorker.getCSRFTokenAndFaculties()
                _faculties.value = facultyList
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось загрузить факультеты: ${e.message}"
            } finally {
                _isLoadingOptions.value = false
            }
        }
    }

    private suspend fun loadDependentOptionsForSelection(selection: UserSelection) {
        if (selection.facultyId.isBlank()) return
        val eduForms = webWorker.fetchEducationForms(selection.facultyId)
        _forms.value = eduForms
        val formId = selection.formId.ifBlank { eduForms.firstOrNull()?.id ?: "Dnevnaya" }

        val courseList = webWorker.fetchCourses(selection.facultyId, formId)
        _courses.value = courseList

        if (selection.courseId.isNotBlank()) {
            val groupList = webWorker.fetchGroups(selection.facultyId, formId, selection.courseId)
            _groups.value = groupList

            if (selection.groupId.isNotBlank()) {
                val weekList = webWorker.fetchWeeks(selection.facultyId, formId, selection.courseId, selection.groupId)
                _weeks.value = processWeekList(weekList)
            }
        }
    }

    private val _allScheduleData = mutableStateOf<List<DaySchedule>>(emptyList())

    private fun processWeekList(weekList: List<OptionItem>): List<OptionItem> {
        if (weekList.size >= 2) {
            val hasAll = weekList.any { it.id == ALL_WEEKS_ID }
            if (!hasAll) {
                return listOf(ALL_WEEKS_OPTION) + weekList
            }
        }
        return weekList
    }

    /** Real weeks excluding the virtual "ALL" option — used for arrows navigation */
    private val realWeeks: List<OptionItem>
        get() = _weeks.value.filter { it.id != ALL_WEEKS_ID }

    val canGoPreviousWeek: Boolean
        get() {
            val list = realWeeks
            if (list.size <= 1) return false
            val currentIndex = list.indexOfFirst { it.id == _userSelection.value.weekId }
            return currentIndex > 0
        }

    val canGoNextWeek: Boolean
        get() {
            val list = realWeeks
            if (list.size <= 1) return false
            val currentIndex = list.indexOfFirst { it.id == _userSelection.value.weekId }
            return currentIndex >= 0 && currentIndex < list.size - 1
        }

    fun selectNextWeek() {
        val list = realWeeks
        val currentIndex = list.indexOfFirst { it.id == _userSelection.value.weekId }
        if (currentIndex >= 0 && currentIndex < list.size - 1) {
            onWeekSelected(list[currentIndex + 1])
        }
    }

    fun selectPreviousWeek() {
        val list = realWeeks
        val currentIndex = list.indexOfFirst { it.id == _userSelection.value.weekId }
        if (currentIndex > 0) {
            onWeekSelected(list[currentIndex - 1])
        }
    }

    fun onFacultySelected(faculty: OptionItem) {
        viewModelScope.launch {
            _isLoadingOptions.value = true
            _userSelection.value = _userSelection.value.copy(
                facultyId = faculty.id,
                facultyName = faculty.name,
                courseId = "",
                courseName = "",
                groupId = "",
                groupName = ""
            )
            _courses.value = emptyList()
            _groups.value = emptyList()
            _weeks.value = emptyList()

            try {
                val eduForms = webWorker.fetchEducationForms(faculty.id)
                _forms.value = eduForms
                val formId = _userSelection.value.formId.ifBlank { eduForms.firstOrNull()?.id ?: "Dnevnaya" }
                val courseList = webWorker.fetchCourses(faculty.id, formId)
                _courses.value = courseList
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при загрузке курсов"
            } finally {
                _isLoadingOptions.value = false
            }
        }
    }

    fun onFormSelected(form: OptionItem) {
        viewModelScope.launch {
            _isLoadingOptions.value = true
            _userSelection.value = _userSelection.value.copy(
                formId = form.id,
                formName = form.name,
                courseId = "",
                courseName = "",
                groupId = "",
                groupName = ""
            )
            _courses.value = emptyList()
            _groups.value = emptyList()
            _weeks.value = emptyList()

            try {
                val courseList = webWorker.fetchCourses(_userSelection.value.facultyId, form.id)
                _courses.value = courseList
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при загрузке курсов"
            } finally {
                _isLoadingOptions.value = false
            }
        }
    }

    fun onCourseSelected(course: OptionItem) {
        viewModelScope.launch {
            _isLoadingOptions.value = true
            _userSelection.value = _userSelection.value.copy(
                courseId = course.id,
                courseName = course.name,
                groupId = "",
                groupName = ""
            )
            _groups.value = emptyList()
            _weeks.value = emptyList()

            try {
                val groupList = webWorker.fetchGroups(
                    _userSelection.value.facultyId,
                    _userSelection.value.formId.ifBlank { "Dnevnaya" },
                    course.id
                )
                _groups.value = groupList
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка при загрузке групп"
            } finally {
                _isLoadingOptions.value = false
            }
        }
    }

    fun onGroupSelected(group: OptionItem) {
        viewModelScope.launch {
            _userSelection.value = _userSelection.value.copy(
                groupId = group.id,
                groupName = group.name,
                weekId = "",
                weekName = ""
            )

            try {
                val weekList = webWorker.fetchWeeks(
                    _userSelection.value.facultyId,
                    _userSelection.value.formId.ifBlank { "Dnevnaya" },
                    _userSelection.value.courseId,
                    group.id
                )
                _weeks.value = processWeekList(weekList)
            } catch (e: Exception) {
                // non-fatal
            }
            fetchSchedule(isManualRefresh = true)
        }
    }

    fun onWeekSelected(week: OptionItem) {
        _userSelection.value = _userSelection.value.copy(
            weekId = week.id,
            weekName = week.name
        )
        viewModelScope.launch {
            preferencesManager.saveSelection(_userSelection.value)
            val hasDataForWeek = if (week.id == ALL_WEEKS_ID) {
                _allScheduleData.value.isNotEmpty()
            } else {
                _allScheduleData.value.any { it.weekId == week.id }
            }

            if (hasDataForWeek) {
                filterAndApplyDisplayedSchedule()
            } else {
                try {
                    _isLoading.value = true
                    val fetched = webWorker.fetchSingleWeekSchedule(_userSelection.value, week.id, week.name)
                    if (fetched.isNotEmpty()) {
                        val existingOtherWeeks = _allScheduleData.value.filter { it.weekId != week.id }
                        val merged = existingOtherWeeks + fetched
                        _allScheduleData.value = merged
                        val timeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        val formattedTime = timeFormat.format(Date())
                        _lastUpdateTime.value = formattedTime
                        preferencesManager.saveSchedule(merged, formattedTime, System.currentTimeMillis())
                    }
                    filterAndApplyDisplayedSchedule()
                } catch (e: Exception) {
                    filterAndApplyDisplayedSchedule()
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    private fun filterAndApplyDisplayedSchedule() {
        val selectedWeekId = _userSelection.value.weekId
        val all = _allScheduleData.value
        val filtered = if (selectedWeekId == ALL_WEEKS_ID) {
            all
        } else {
            // Filter strictly by week: if the selected week has no data, show empty state
            all.filter { it.weekId == selectedWeekId }
        }
        _scheduleData.value = filtered.map { day ->
            day.copy(lessons = WebWorker.groupLessonsByTime(day.lessons.map { WebWorker.normalizeLesson(it) }))
        }
    }

    fun applySelection(selection: UserSelection) {
        _userSelection.value = selection
        viewModelScope.launch {
            preferencesManager.saveSelection(selection)
            fetchSchedule(isManualRefresh = true)
        }
    }

    fun fetchSchedule(isManualRefresh: Boolean = true) {
        val currentSelection = _userSelection.value
        if (!currentSelection.isComplete) {
            _errorMessage.value = "Сначала выберите группу"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = webWorker.fetchScheduleAndAvailableWeeks(currentSelection)
                if (result.weeks.isNotEmpty()) {
                    val processed = processWeekList(result.weeks)
                    _weeks.value = processed

                    val current = _userSelection.value
                    if (current.weekId != ALL_WEEKS_ID) {
                        val knownIds = result.weeks.map { it.id }
                        val currentWeekId = PreferencesManager.findCurrentWeekId(result.weeks, result.daySchedules)

                        val currentWeekHasLessons = result.daySchedules.any { it.weekId == current.weekId }
                        val shouldResetToCurrentWeek = isManualRefresh ||
                                current.weekId.isBlank() ||
                                current.weekId == "0" ||
                                current.weekId !in knownIds ||
                                (!currentWeekHasLessons && currentWeekId != null)

                        val targetWeekId = if (shouldResetToCurrentWeek) {
                            currentWeekId ?: result.weeks.firstOrNull { it.id != ALL_WEEKS_ID }?.id ?: result.weeks.firstOrNull()?.id
                        } else {
                            current.weekId
                        }

                        val resolved = result.weeks.find { it.id == targetWeekId }
                        if (resolved != null && (resolved.id != current.weekId || resolved.name != current.weekName)) {
                            val updated = current.copy(weekId = resolved.id, weekName = resolved.name)
                            _userSelection.value = updated
                            preferencesManager.saveSelection(updated)
                        }
                    }
                }

                if (result.daySchedules.isNotEmpty()) {
                    _allScheduleData.value = result.daySchedules
                    filterAndApplyDisplayedSchedule()

                    val timeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    val formattedTime = timeFormat.format(Date())
                    _lastUpdateTime.value = formattedTime
                    preferencesManager.saveSchedule(result.daySchedules, formattedTime, System.currentTimeMillis())
                } else {
                    if (_scheduleData.value.isEmpty()) {
                        _errorMessage.value = "Занятия не найдены или расписание не опубликовано."
                    }
                }
            } catch (e: Exception) {
                if (_scheduleData.value.isEmpty()) {
                    _errorMessage.value = "Не удалось загрузить расписание: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ----------------- Student Cabinet Methods -----------------

    fun loginStudent(login: String, pass: String, rememberMe: Boolean) {
        viewModelScope.launch {
            _isStudentLoading.value = true
            _studentErrorMessage.value = null

            val result = studentWebWorker.loginAndFetch(login, pass)
            result.onSuccess { data ->
                _studentCabinetData.value = data
                val creds = StudentAuthCredentials(login, pass, rememberMe)
                savedCredentials = creds

                if (rememberMe) {
                    preferencesManager.saveStudentCredentials(creds)
                    preferencesManager.saveStudentCabinetData(data)
                }
            }.onFailure { exception ->
                _studentErrorMessage.value = exception.message ?: "Ошибка авторизации"
            }

            _isStudentLoading.value = false
        }
    }

    fun refreshStudentCabinet() {
        val creds = savedCredentials ?: return
        viewModelScope.launch {
            _isStudentLoading.value = true
            _studentErrorMessage.value = null

            val result = studentWebWorker.loginAndFetch(creds.login, creds.password)
            result.onSuccess { data ->
                _studentCabinetData.value = data
                if (creds.rememberMe) {
                    preferencesManager.saveStudentCabinetData(data)
                }
            }.onFailure { exception ->
                _studentErrorMessage.value = exception.message
            }

            _isStudentLoading.value = false
        }
    }

    fun logoutStudent() {
        viewModelScope.launch {
            _studentCabinetData.value = null
            savedCredentials = null
            preferencesManager.clearStudentSession()
        }
    }

    // ----------------- Settings & Server Methods -----------------

    fun checkServerHealth() {
        viewModelScope.launch {
            _isCheckingHealth.value = true
            try {
                val health = webWorker.checkHealth()
                _serverHealth.value = health
            } catch (e: Exception) {
                _serverHealth.value = ServerHealth(status = "error", service = "university", version = "")
            } finally {
                _isCheckingHealth.value = false
            }
        }
    }

    fun updateServerUrl(newUrl: String) {
        val sanitized = newUrl.trim().trimEnd('/')
        if (sanitized.isNotBlank()) {
            _serverUrl.value = sanitized
            webWorker.updateBaseUrl(sanitized)
            studentWebWorker.updateBaseUrl(sanitized)
            viewModelScope.launch {
                preferencesManager.saveServerUrl(sanitized)
                checkServerHealth()
                loadFaculties()
            }
        }
    }

    fun clearScheduleCache() {
        viewModelScope.launch {
            preferencesManager.clearScheduleCache()
            _allScheduleData.value = emptyList()
            _scheduleData.value = emptyList()
            _lastUpdateTime.value = null
            // Reset to current week
            val resetSelection = _userSelection.value.copy(weekId = "0", weekName = "Текущая неделя")
            _userSelection.value = resetSelection
            preferencesManager.saveSelection(resetSelection)
            if (resetSelection.isComplete) {
                fetchSchedule(isManualRefresh = true)
            }
        }
    }

    fun resetSelection() {
        viewModelScope.launch {
            preferencesManager.clearSelection()
            _userSelection.value = UserSelection()
            _courses.value = emptyList()
            _groups.value = emptyList()
            _weeks.value = emptyList()
            clearScheduleCache()
        }
    }
}