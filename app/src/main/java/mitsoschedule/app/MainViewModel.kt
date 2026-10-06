package mitsoschedule.app

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import mitsoschedule.app.data.AppSettings
import mitsoschedule.app.data.PreferencesManager
import mitsoschedule.core.model.DaySchedule
import mitsoschedule.core.model.OptionItem
import mitsoschedule.core.model.ServerHealth
import mitsoschedule.core.model.StudentCabinetData
import mitsoschedule.core.model.UserSelection
import mitsoschedule.core.network.ApiError
import mitsoschedule.core.network.ApiResult
import mitsoschedule.core.network.StudentSession
import mitsoschedule.core.network.orEmpty
import mitsoschedule.core.schedule.ScheduleDates
import mitsoschedule.core.schedule.ScheduleNormalizer
import mitsoschedule.core.schedule.ScheduleRepository
import mitsoschedule.core.schedule.Weeks
import mitsoschedule.core.storage.ScheduleStore
import mitsoschedule.core.ui.UiStrings
import mitsoschedule.core.ui.reason

class MainViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val studentSession: StudentSession,
    private val store: ScheduleStore,
    private val settings: AppSettings,
    private val strings: UiStrings,
    private val onScheduleUpdated: () -> Unit = {}
) : ViewModel() {

    companion object {
        const val ALL_WEEKS_ID = Weeks.ALL_ID
        val ALL_WEEKS_OPTION = Weeks.ALL_OPTION
    }

    // Navigation state
    private val _currentTab = mutableStateOf(AppTab.SCHEDULE)
    val currentTab: State<AppTab> = _currentTab

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
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

    private val _useDynamicColor = mutableStateOf(false)
    val useDynamicColor: State<Boolean> = _useDynamicColor

    fun setDynamicColor(enabled: Boolean) {
        _useDynamicColor.value = enabled
        viewModelScope.launch {
            settings.setDynamicColor(enabled)
        }
    }

    private val _allScheduleData = mutableStateOf<List<DaySchedule>>(emptyList())

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        viewModelScope.launch {
            settings.setThemeMode(mode)
        }
    }

    init {
        loadInitialData()
        loadInitialStudentData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // 0. Load server URL and theme from preferences
            val savedServerUrl = settings.serverUrlFlow.firstOrNull() ?: PreferencesManager.DEFAULT_SERVER_URL
            _serverUrl.value = savedServerUrl
            scheduleRepository.setServerUrl(savedServerUrl)
            studentSession.setServerUrl(savedServerUrl)
            checkServerHealth()

            _themeMode.value = settings.themeModeFlow.firstOrNull() ?: "system"
            _useDynamicColor.value = settings.dynamicColorFlow.firstOrNull() ?: false

            // 1. Read local preferences immediately
            val saved = store.savedSelectionFlow.firstOrNull()
            val savedTime = store.lastUpdateFlow.firstOrNull()
            val cachedSchedule = store.cachedScheduleFlow.firstOrNull()
            val lastFetchMillis = store.lastFetchMillisFlow.firstOrNull() ?: 0L

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
                val cachedWeeks = Weeks.fromSchedule(cachedSchedule)
                if (cachedWeeks.isNotEmpty()) {
                    _weeks.value = Weeks.withAllOption(cachedWeeks)
                }

                // If saved week is invalid or default "0" or blank, resolve to current week
                if (saved != null) {
                    Weeks.resolveStored(saved, _weeks.value, cachedSchedule, requireKnown = true)?.let {
                        _userSelection.value = it
                    }
                }

                // Filter and display schedule instantly on screen
                filterAndApplyDisplayedSchedule()
            }

            // 3. Load options and trigger background refresh asynchronously without blocking cached UI
            launch {
                // Группа уже выбрана и расписание показано из кэша: сбой фоновой загрузки
                // списка факультетов (например, нет сети) не должен выглядеть как ошибка экрана
                loadFaculties(reportErrors = saved?.isComplete != true)
            }

            if (saved != null && saved.isComplete) {
                launch {
                    // офлайн-кэш важнее: ошибку загрузки вариантов выбора не показываем
                    loadDependentOptionsForSelection(saved)
                }

                val isStale = ScheduleDates.isScheduleOlderThanWeek(lastFetchMillis, cachedSchedule ?: emptyList())
                val shouldRefresh = ScheduleDates.shouldAutoRefresh(lastFetchMillis)
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
            val lastFetch = store.getLastFetchMillis()
            val cached = _allScheduleData.value.ifEmpty {
                store.cachedScheduleFlow.firstOrNull() ?: emptyList()
            }
            if (force || ScheduleDates.isScheduleOlderThanWeek(lastFetch, cached)) {
                fetchSchedule(isManualRefresh = true)
            }
        }
    }

    private fun loadInitialStudentData() {
        viewModelScope.launch {
            studentSession.migrateLegacySecrets()

            // Load cached cabinet data if any
            studentSession.cachedData()?.let { _studentCabinetData.value = it }

            // Load credentials and auto-refresh
            if (studentSession.restore(requireRemember = true) != null) {
                refreshStudentCabinet()
            }
        }
    }

    // ----------------- Schedule Methods -----------------

    fun loadFaculties(reportErrors: Boolean = true) {
        viewModelScope.launch {
            _isLoadingOptions.value = true
            when (val result = scheduleRepository.faculties()) {
                is ApiResult.Success -> _faculties.value = result.value
                is ApiResult.Failure -> if (reportErrors) {
                    _errorMessage.value = strings.get(R.string.error_load_faculties, strings.reason(result.error))
                }
            }
            _isLoadingOptions.value = false
        }
    }

    private suspend fun loadDependentOptionsForSelection(selection: UserSelection) {
        if (selection.facultyId.isBlank()) return
        val options = scheduleRepository.loadOptions(selection)
        _forms.value = options.forms
        _courses.value = options.courses
        if (selection.courseId.isNotBlank()) _groups.value = options.groups
        if (selection.groupId.isNotBlank()) _weeks.value = options.weeks
    }

    val canGoPreviousWeek: Boolean
        get() = Weeks.hasPrevious(_weeks.value, _userSelection.value.weekId)

    val canGoNextWeek: Boolean
        get() = Weeks.hasNext(_weeks.value, _userSelection.value.weekId)

    fun selectNextWeek() {
        Weeks.next(_weeks.value, _userSelection.value.weekId)?.let(::onWeekSelected)
    }

    fun selectPreviousWeek() {
        Weeks.previous(_weeks.value, _userSelection.value.weekId)?.let(::onWeekSelected)
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

            val eduForms = scheduleRepository.forms(faculty.id)
            _forms.value = eduForms.orEmpty()
            val formId = _userSelection.value.formId.ifBlank { eduForms.orEmpty().firstOrNull()?.id ?: "Dnevnaya" }
            val courseList = scheduleRepository.courses(faculty.id, formId)
            _courses.value = courseList.orEmpty()
            reportCoursesError(eduForms.errorOrNull ?: courseList.errorOrNull)
            _isLoadingOptions.value = false
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

            val courseList = scheduleRepository.courses(_userSelection.value.facultyId, form.id)
            _courses.value = courseList.orEmpty()
            reportCoursesError(courseList.errorOrNull)
            _isLoadingOptions.value = false
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

            val groupList = scheduleRepository.groups(
                _userSelection.value.facultyId,
                _userSelection.value.formId.ifBlank { "Dnevnaya" },
                course.id
            )
            _groups.value = groupList.orEmpty()
            groupList.errorOrNull?.let {
                _errorMessage.value = strings.get(R.string.error_load_groups, strings.reason(it))
            }
            _isLoadingOptions.value = false
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

            // список недель не критичен: расписание загрузится и без него
            val weekList = scheduleRepository.weeks(
                _userSelection.value.facultyId,
                _userSelection.value.formId.ifBlank { "Dnevnaya" },
                _userSelection.value.courseId,
                group.id
            )
            _weeks.value = Weeks.withAllOption(weekList.orEmpty())
            fetchSchedule(isManualRefresh = true)
        }
    }

    fun onWeekSelected(week: OptionItem) {
        _userSelection.value = _userSelection.value.copy(
            weekId = week.id,
            weekName = week.name
        )
        viewModelScope.launch {
            store.saveSelection(_userSelection.value)
            val hasDataForWeek = if (week.id == ALL_WEEKS_ID) {
                _allScheduleData.value.isNotEmpty()
            } else {
                _allScheduleData.value.any { it.weekId == week.id }
            }

            if (!hasDataForWeek) {
                _isLoading.value = true
                val loaded = scheduleRepository.loadWeek(_userSelection.value, week, _allScheduleData.value)
                if (loaded is ApiResult.Success) {
                    loaded.value?.let { (merged, time) ->
                        _allScheduleData.value = merged
                        _lastUpdateTime.value = time
                        onScheduleUpdated()
                    }
                }
                _isLoading.value = false
            }
            filterAndApplyDisplayedSchedule()
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
            day.copy(lessons = ScheduleNormalizer.groupLessonsByTime(day.lessons.map { ScheduleNormalizer.normalizeLesson(it) }))
        }
    }

    fun applySelection(selection: UserSelection) {
        _userSelection.value = selection
        viewModelScope.launch {
            store.saveSelection(selection)
            fetchSchedule(isManualRefresh = true)
        }
    }

    fun fetchSchedule(isManualRefresh: Boolean = true) {
        val currentSelection = _userSelection.value
        if (!currentSelection.isComplete) {
            _errorMessage.value = strings.get(R.string.error_select_group_first)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = scheduleRepository.refresh(currentSelection, isManualRefresh)
            if (result.weeks.isNotEmpty()) {
                _weeks.value = result.weeks
            }
            _userSelection.value = result.selection

            if (result.days.isNotEmpty()) {
                _allScheduleData.value = result.days
                filterAndApplyDisplayedSchedule()
                _lastUpdateTime.value = result.updateTime
                onScheduleUpdated()
            } else if (_scheduleData.value.isEmpty()) {
                // сбой сети и «расписание не опубликовано» это разные ситуации
                _errorMessage.value = result.error
                    ?.let { strings.get(R.string.error_schedule_load, strings.reason(it)) }
                    ?: strings.get(R.string.error_schedule_empty)
            }
            _isLoading.value = false
        }
    }

    private fun reportCoursesError(error: ApiError?) {
        error?.let { _errorMessage.value = strings.get(R.string.error_load_courses, strings.reason(it)) }
    }

    // ----------------- Student Cabinet Methods -----------------

    fun loginStudent(login: String, pass: String, rememberMe: Boolean) {
        viewModelScope.launch {
            _isStudentLoading.value = true
            _studentErrorMessage.value = null

            studentSession.login(login, pass, rememberMe)
                .onSuccess { data -> _studentCabinetData.value = data }
                .onFailure { exception ->
                    _studentErrorMessage.value = exception.message ?: strings.get(R.string.error_auth_default)
                }

            _isStudentLoading.value = false
        }
    }

    fun refreshStudentCabinet() {
        if (!studentSession.isSignedIn) return
        viewModelScope.launch {
            _isStudentLoading.value = true
            _studentErrorMessage.value = null

            studentSession.refresh()
                ?.onSuccess { data -> _studentCabinetData.value = data }
                ?.onFailure { exception -> _studentErrorMessage.value = exception.message }

            _isStudentLoading.value = false
        }
    }

    fun logoutStudent() {
        viewModelScope.launch {
            _studentCabinetData.value = null
            studentSession.logout()
        }
    }

    // ----------------- Settings & Server Methods -----------------

    fun checkServerHealth() {
        viewModelScope.launch {
            _isCheckingHealth.value = true
            _serverHealth.value = scheduleRepository.checkHealth()
            _isCheckingHealth.value = false
        }
    }

    fun updateServerUrl(newUrl: String) {
        val sanitized = newUrl.trim().trimEnd('/')
        if (sanitized.isNotBlank()) {
            _serverUrl.value = sanitized
            scheduleRepository.setServerUrl(sanitized)
            studentSession.setServerUrl(sanitized)
            viewModelScope.launch {
                settings.saveServerUrl(sanitized)
                checkServerHealth()
                loadFaculties()
            }
        }
    }

    fun clearScheduleCache() {
        viewModelScope.launch {
            store.clearScheduleCache()
            _allScheduleData.value = emptyList()
            _scheduleData.value = emptyList()
            _lastUpdateTime.value = null
            // Reset to current week
            val defaults = UserSelection()
            val resetSelection = _userSelection.value.copy(weekId = defaults.weekId, weekName = defaults.weekName)
            _userSelection.value = resetSelection
            store.saveSelection(resetSelection)
            if (resetSelection.isComplete) {
                fetchSchedule(isManualRefresh = true)
            }
        }
    }

    fun resetSelection() {
        viewModelScope.launch {
            store.clearSelection()
            _userSelection.value = UserSelection()
            _courses.value = emptyList()
            _groups.value = emptyList()
            _weeks.value = emptyList()
            clearScheduleCache()
        }
    }
}
