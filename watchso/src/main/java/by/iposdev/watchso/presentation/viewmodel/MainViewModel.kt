package by.iposdev.watchso.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import by.iposdev.watchso.data.DaySchedule
import by.iposdev.watchso.data.OptionItem
import by.iposdev.watchso.data.ScheduleTimeUtils
import by.iposdev.watchso.data.StudentAuthCredentials
import by.iposdev.watchso.data.StudentCabinetData
import by.iposdev.watchso.data.StudentWebWorker
import by.iposdev.watchso.data.UserSelection
import by.iposdev.watchso.data.WatchPreferencesManager
import by.iposdev.watchso.data.WebWorker
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class WearPickerStep {
    FACULTY,
    COURSE,
    GROUP
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val ALL_WEEKS_ID = "ALL"
        val ALL_WEEKS_OPTION = OptionItem(id = ALL_WEEKS_ID, name = "Все доступные недели")
    }

    private val tag = "WatchMainViewModel"
    private val webWorker = WebWorker(application.applicationContext)
    private val studentWebWorker = StudentWebWorker()
    private val preferencesManager = WatchPreferencesManager(application.applicationContext)

    // Active Screen: 0 = Schedule, 1 = Cabinet, 2 = Group Picker
    private val _currentScreen = mutableIntStateOf(0)
    val currentScreen: State<Int> = _currentScreen

    private val _pickerStep = mutableStateOf(WearPickerStep.FACULTY)
    val pickerStep: State<WearPickerStep> = _pickerStep

    fun setPickerStep(step: WearPickerStep) {
        _pickerStep.value = step
    }

    fun navigateTo(screen: Int) {
        _currentScreen.intValue = screen
        if (screen == 2) {
            val isOldFormat = !_userSelection.value.hasValidFormat && _userSelection.value.isComplete
            if (isOldFormat) {
                _userSelection.value = UserSelection()
                viewModelScope.launch { preferencesManager.saveSelection(UserSelection()) }
            }
            if (_faculties.value.isEmpty()) {
                loadFaculties()
            }
            _pickerStep.value = when {
                _userSelection.value.facultyId.isBlank() -> WearPickerStep.FACULTY
                _courses.value.isNotEmpty() && _userSelection.value.courseId.isNotBlank() && _groups.value.isNotEmpty() -> WearPickerStep.GROUP
                _courses.value.isNotEmpty() -> WearPickerStep.COURSE
                else -> {
                    viewModelScope.launch { loadDependentOptions(_userSelection.value) }
                    WearPickerStep.FACULTY
                }
            }
        } else if (screen == 1 && _studentCabinetData.value == null && savedCredentials != null) {
            refreshStudentCabinet()
        }
    }

    // Schedule UI States
    private val _userSelection = mutableStateOf(UserSelection())
    val userSelection: State<UserSelection> = _userSelection

    private val _allScheduleData = mutableStateOf<List<DaySchedule>>(emptyList())
    val allScheduleData: State<List<DaySchedule>> = _allScheduleData

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

    val realWeeks: List<OptionItem>
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

    private var savedCredentials: StudentAuthCredentials? = null

    init {
        loadInitialSchedule()
        loadInitialStudent()
    }

    private fun loadInitialSchedule() {
        viewModelScope.launch {
            // 1. Instant display from local DataStore cache (0ms delay)
            val cached = preferencesManager.cachedScheduleFlow.firstOrNull() ?: emptyList()
            val lastFetchMillis = preferencesManager.lastFetchMillisFlow.firstOrNull() ?: 0L
            if (cached.isNotEmpty()) {
                _allScheduleData.value = cached
                val cachedWeeks = cached
                    .map { OptionItem(it.weekId, it.weekName) }
                    .filter { it.id.isNotBlank() && it.id != "0" }
                    .distinctBy { it.id }
                if (cachedWeeks.isNotEmpty()) {
                    _weeks.value = processWeekList(cachedWeeks)
                }
            }
            val savedTime = preferencesManager.lastUpdateFlow.firstOrNull()
            if (savedTime != null) {
                _lastUpdateTime.value = savedTime
            }

            // 2. Load saved user selection
            val savedSelection = preferencesManager.savedSelectionFlow.firstOrNull()
            if (savedSelection != null && !savedSelection.hasValidFormat && savedSelection.isComplete) {
                Log.w(tag, "Detected obsolete numeric selection, resetting")
                val emptySelection = UserSelection()
                _userSelection.value = emptySelection
                preferencesManager.saveSelection(emptySelection)
                loadFaculties()
            } else if (savedSelection != null && savedSelection.isComplete) {
                _userSelection.value = savedSelection
                if (cached.isNotEmpty()) {
                    filterAndApplyDisplayedSchedule()
                }

                // 3. Forced sync if cache is older than a week, or normal refresh policy
                val isStale = WatchPreferencesManager.isScheduleOlderThanWeek(lastFetchMillis, cached)
                val shouldRefresh = WatchPreferencesManager.shouldAutoRefresh(lastFetchMillis)
                if (isStale || shouldRefresh || cached.isEmpty()) {
                    fetchSchedule(isManualRefresh = isStale)
                }
            } else {
                loadFaculties()
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
                preferencesManager.getCachedSchedule()
            }
            if (force || WatchPreferencesManager.isScheduleOlderThanWeek(lastFetch, cached)) {
                fetchSchedule(isManualRefresh = true)
            }
        }
    }

    private fun loadInitialStudent() {
        viewModelScope.launch {
            // Instant load of cached cabinet
            val cachedData = preferencesManager.cachedStudentCabinetFlow.firstOrNull()
            if (cachedData != null) {
                _studentCabinetData.value = cachedData
            }

            val credentials = preferencesManager.studentCredentialsFlow.firstOrNull()
            if (credentials != null && credentials.login.isNotBlank()) {
                savedCredentials = credentials
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
                Log.e(tag, "Failed to load faculties", e)
            } finally {
                _isLoadingOptions.value = false
            }
        }
    }

    private suspend fun loadDependentOptions(selection: UserSelection) {
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
                _weeks.value = weekList
            }
        }
    }

    fun onFacultySelected(faculty: OptionItem) {
        viewModelScope.launch {
            _isLoadingOptions.value = true
            _userSelection.value = _userSelection.value.copy(
                facultyId = faculty.id,
                facultyName = faculty.name,
                formId = "Dnevnaya",
                formName = "Дневная",
                courseId = "",
                courseName = "",
                groupId = "",
                groupName = ""
            )
            _courses.value = emptyList()
            _groups.value = emptyList()
            _weeks.value = emptyList()
            _pickerStep.value = WearPickerStep.COURSE

            try {
                val eduForms = webWorker.fetchEducationForms(faculty.id)
                _forms.value = eduForms
                val formId = eduForms.firstOrNull()?.id ?: "Dnevnaya"
                _userSelection.value = _userSelection.value.copy(
                    formId = formId,
                    formName = eduForms.firstOrNull()?.name ?: "Дневная"
                )
                val courseList = webWorker.fetchCourses(faculty.id, formId)
                _courses.value = courseList
            } catch (e: Exception) {
                Log.e(tag, "Failed to load courses", e)
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
                Log.e(tag, "Failed to load courses", e)
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
            _pickerStep.value = WearPickerStep.GROUP

            try {
                val formId = _userSelection.value.formId.ifBlank { "Dnevnaya" }
                val groupList = webWorker.fetchGroups(
                    _userSelection.value.facultyId,
                    formId,
                    course.id
                )
                _groups.value = groupList
            } catch (e: Exception) {
                Log.e(tag, "Failed to load groups", e)
            } finally {
                _isLoadingOptions.value = false
            }
        }
    }

    private fun processWeekList(weekList: List<OptionItem>): List<OptionItem> {
        if (weekList.size >= 2) {
            val hasAll = weekList.any { it.id == ALL_WEEKS_ID }
            if (!hasAll) {
                return listOf(ALL_WEEKS_OPTION) + weekList
            }
        }
        return weekList
    }

    fun onGroupSelected(group: OptionItem) {
        viewModelScope.launch {
            val updated = _userSelection.value.copy(
                groupId = group.id,
                groupName = group.name,
                weekId = "",
                weekName = ""
            )
            _userSelection.value = updated
            preferencesManager.saveSelection(updated)

            _pickerStep.value = WearPickerStep.FACULTY
            _currentScreen.intValue = 0 // Navigate back to schedule

            try {
                val formId = updated.formId.ifBlank { "Dnevnaya" }
                val weekList = webWorker.fetchWeeks(
                    updated.facultyId,
                    formId,
                    updated.courseId,
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
            if (_allScheduleData.value.isNotEmpty()) {
                filterAndApplyDisplayedSchedule()
            } else {
                fetchSchedule(isManualRefresh = true)
            }
        }
        _currentScreen.intValue = 0
    }

    private fun filterAndApplyDisplayedSchedule() {
        val all = _allScheduleData.value
        if (all.isEmpty()) {
            _scheduleData.value = emptyList()
            return
        }

        var selectedWeekId = _userSelection.value.weekId
        if (selectedWeekId.isBlank() || selectedWeekId == "0") {
            val resolvedId = ScheduleTimeUtils.findCurrentWeekId(_weeks.value, all)
            if (resolvedId != null) {
                selectedWeekId = resolvedId
                val resolvedName = _weeks.value.find { it.id == resolvedId }?.name
                    ?: all.firstOrNull { it.weekId == resolvedId }?.weekName ?: ""
                _userSelection.value = _userSelection.value.copy(weekId = resolvedId, weekName = resolvedName)
                viewModelScope.launch { preferencesManager.saveSelection(_userSelection.value) }
            }
        }

        val filtered = if (selectedWeekId == ALL_WEEKS_ID) {
            all
        } else {
            all.filter { it.weekId == selectedWeekId }
        }
        _scheduleData.value = filtered
    }

    fun fetchSchedule(isManualRefresh: Boolean = true) {
        val current = _userSelection.value
        if (!current.isComplete) {
            _errorMessage.value = "Выберите группу"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // 1. Fetch weeks list
                val freshWeeks = webWorker.fetchWeeks(
                    current.facultyId,
                    current.formId.ifBlank { "Dnevnaya" },
                    current.courseId,
                    current.groupId
                )
                if (freshWeeks.isNotEmpty()) {
                    val processed = processWeekList(freshWeeks)
                    _weeks.value = processed
                }

                // 2. Fetch ALL weeks schedule in one request!
                val result = webWorker.fetchScheduleForWeeks(current, emptyList())

                if (result.isNotEmpty()) {
                    _allScheduleData.value = result

                    // Auto-resolve week to current week if empty or invalid
                    val activeSelection = _userSelection.value
                    if (activeSelection.weekId != ALL_WEEKS_ID) {
                        val activeWeeks = _weeks.value.filter { it.id != ALL_WEEKS_ID }
                        val weekExists = activeWeeks.any { it.id == activeSelection.weekId }
                        if (!weekExists || activeSelection.weekId.isBlank() || activeSelection.weekId == "0") {
                            val resolvedId = ScheduleTimeUtils.findCurrentWeekId(_weeks.value, result)
                            if (resolvedId != null) {
                                val resolvedName = _weeks.value.find { it.id == resolvedId }?.name
                                    ?: result.firstOrNull { it.weekId == resolvedId }?.weekName ?: ""
                                _userSelection.value = activeSelection.copy(weekId = resolvedId, weekName = resolvedName)
                                preferencesManager.saveSelection(_userSelection.value)
                            }
                        } else {
                            val matched = activeWeeks.find { it.id == activeSelection.weekId }
                            if (matched != null && matched.name != activeSelection.weekName) {
                                _userSelection.value = activeSelection.copy(weekName = matched.name)
                                preferencesManager.saveSelection(_userSelection.value)
                            }
                        }
                    }

                    filterAndApplyDisplayedSchedule()

                    val timeFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
                    val formattedTime = timeFormat.format(Date())
                    _lastUpdateTime.value = formattedTime
                    preferencesManager.saveSchedule(result, formattedTime, System.currentTimeMillis())
                    notifyTileAndComplications()
                } else {
                    if (_allScheduleData.value.isEmpty()) {
                        _errorMessage.value = "Расписание не найдено"
                    }
                }
            } catch (e: Exception) {
                if (_allScheduleData.value.isEmpty()) {
                    _errorMessage.value = "Ошибка загрузки: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ----------------- Student Cabinet Methods -----------------

    fun loginStudent(login: String, pass: String) {
        viewModelScope.launch {
            _isStudentLoading.value = true
            _studentErrorMessage.value = null

            val result = studentWebWorker.loginAndFetch(login, pass)
            result.onSuccess { data ->
                _studentCabinetData.value = data
                val creds = StudentAuthCredentials(login, pass, rememberMe = true)
                savedCredentials = creds
                preferencesManager.saveStudentCredentials(creds)
                preferencesManager.saveStudentCabinetData(data)
            }.onFailure { exception ->
                _studentErrorMessage.value = exception.message ?: "Ошибка входа"
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
                preferencesManager.saveStudentCabinetData(data)
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

    private fun notifyTileAndComplications() {
        try {
            val app = getApplication<Application>()
            androidx.wear.tiles.TileService.getUpdater(app)
                .requestUpdate(by.iposdev.watchso.tile.ScheduleTileService::class.java)
            androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester.create(
                app,
                android.content.ComponentName(app, by.iposdev.watchso.complication.MainComplicationService::class.java)
            ).requestUpdateAll()
        } catch (_: Exception) {
        }
    }
}
