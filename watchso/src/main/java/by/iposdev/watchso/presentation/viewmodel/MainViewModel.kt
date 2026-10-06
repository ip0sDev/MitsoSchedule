package by.iposdev.watchso.presentation.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.iposdev.watchso.R
import by.iposdev.watchso.presentation.WearScreen
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import mitsoschedule.core.model.DaySchedule
import mitsoschedule.core.model.OptionItem
import mitsoschedule.core.model.StudentCabinetData
import mitsoschedule.core.model.UserSelection
import mitsoschedule.core.network.ApiResult
import mitsoschedule.core.network.StudentSession
import mitsoschedule.core.network.orEmpty
import mitsoschedule.core.schedule.ScheduleDates
import mitsoschedule.core.schedule.ScheduleRepository
import mitsoschedule.core.schedule.Weeks
import mitsoschedule.core.storage.ScheduleStore
import mitsoschedule.core.ui.UiStrings
import mitsoschedule.core.ui.reason

enum class WearPickerStep {
    FACULTY,
    COURSE,
    GROUP
}

/**
 * @param onScheduleUpdated вызывается после сохранения свежего расписания:
 *   обновляет плитку и компликации (в ViewModel нет Context).
 */
class MainViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val studentSession: StudentSession,
    private val store: ScheduleStore,
    private val strings: UiStrings,
    private val onScheduleUpdated: () -> Unit = {}
) : ViewModel() {

    companion object {
        const val ALL_WEEKS_ID = Weeks.ALL_ID
        val ALL_WEEKS_OPTION = Weeks.ALL_OPTION
    }

    private val tag = "WatchMainViewModel"

    private val _currentScreen = mutableStateOf(WearScreen.SCHEDULE)
    val currentScreen: State<WearScreen> = _currentScreen

    private val _pickerStep = mutableStateOf(WearPickerStep.FACULTY)
    val pickerStep: State<WearPickerStep> = _pickerStep

    fun setPickerStep(step: WearPickerStep) {
        _pickerStep.value = step
    }

    fun navigateTo(screen: WearScreen) {
        _currentScreen.value = screen
        if (screen == WearScreen.PICKER) {
            val isOldFormat = !_userSelection.value.hasValidFormat && _userSelection.value.isComplete
            if (isOldFormat) {
                _userSelection.value = UserSelection()
                viewModelScope.launch { store.saveSelection(UserSelection()) }
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
        } else if (screen == WearScreen.CABINET && _studentCabinetData.value == null && studentSession.isSignedIn) {
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
        get() = Weeks.real(_weeks.value)

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

    init {
        loadInitialSchedule()
        loadInitialStudent()
    }

    private fun loadInitialSchedule() {
        viewModelScope.launch {
            // 1. Instant display from local DataStore cache (0ms delay)
            val cached = store.cachedScheduleFlow.firstOrNull() ?: emptyList()
            val lastFetchMillis = store.lastFetchMillisFlow.firstOrNull() ?: 0L
            if (cached.isNotEmpty()) {
                _allScheduleData.value = cached
                val cachedWeeks = Weeks.fromSchedule(cached)
                if (cachedWeeks.isNotEmpty()) {
                    _weeks.value = Weeks.withAllOption(cachedWeeks)
                }
            }
            val savedTime = store.lastUpdateFlow.firstOrNull()
            if (savedTime != null) {
                _lastUpdateTime.value = savedTime
            }

            // 2. Load saved user selection
            val savedSelection = store.savedSelectionFlow.firstOrNull()
            if (savedSelection != null && !savedSelection.hasValidFormat && savedSelection.isComplete) {
                Log.w(tag, "Detected obsolete numeric selection, resetting")
                val emptySelection = UserSelection()
                _userSelection.value = emptySelection
                store.saveSelection(emptySelection)
                loadFaculties()
            } else if (savedSelection != null && savedSelection.isComplete) {
                _userSelection.value = savedSelection
                if (cached.isNotEmpty()) {
                    filterAndApplyDisplayedSchedule()
                }

                // 3. Forced sync if cache is older than a week, or normal refresh policy
                val isStale = ScheduleDates.isScheduleOlderThanWeek(lastFetchMillis, cached)
                val shouldRefresh = ScheduleDates.shouldAutoRefresh(lastFetchMillis)
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
            val lastFetch = store.getLastFetchMillis()
            val cached = _allScheduleData.value.ifEmpty {
                store.getCachedSchedule()
            }
            if (force || ScheduleDates.isScheduleOlderThanWeek(lastFetch, cached)) {
                fetchSchedule(isManualRefresh = true)
            }
        }
    }

    private fun loadInitialStudent() {
        viewModelScope.launch {
            studentSession.migrateLegacySecrets()

            // Instant load of cached cabinet
            studentSession.cachedData()?.let { _studentCabinetData.value = it }

            studentSession.restore(requireRemember = false)
        }
    }

    // ----------------- Schedule Methods -----------------

    fun loadFaculties() {
        viewModelScope.launch {
            _isLoadingOptions.value = true
            when (val result = scheduleRepository.faculties()) {
                is ApiResult.Success -> _faculties.value = result.value
                is ApiResult.Failure -> {
                    Log.e(tag, "Failed to load faculties: ${result.error}")
                    _errorMessage.value = strings.get(R.string.error_load_faculties, strings.reason(result.error))
                }
            }
            _isLoadingOptions.value = false
        }
    }

    private suspend fun loadDependentOptions(selection: UserSelection) {
        if (selection.facultyId.isBlank()) return
        val options = scheduleRepository.loadOptions(selection)
        _forms.value = options.forms
        _courses.value = options.courses
        if (selection.courseId.isNotBlank()) _groups.value = options.groups
        if (selection.groupId.isNotBlank()) _weeks.value = options.weeks
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

            val eduForms = scheduleRepository.forms(faculty.id)
            _forms.value = eduForms.orEmpty()
            val formId = eduForms.orEmpty().firstOrNull()?.id ?: "Dnevnaya"
            _userSelection.value = _userSelection.value.copy(
                formId = formId,
                formName = eduForms.orEmpty().firstOrNull()?.name ?: "Дневная"
            )
            val courseList = scheduleRepository.courses(faculty.id, formId)
            _courses.value = courseList.orEmpty()
            (eduForms.errorOrNull ?: courseList.errorOrNull)?.let(::reportCoursesError)
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
            courseList.errorOrNull?.let(::reportCoursesError)
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
            _pickerStep.value = WearPickerStep.GROUP

            val groupList = scheduleRepository.groups(
                _userSelection.value.facultyId,
                _userSelection.value.formId.ifBlank { "Dnevnaya" },
                course.id
            )
            _groups.value = groupList.orEmpty()
            groupList.errorOrNull?.let {
                Log.e(tag, "Failed to load groups: $it")
                _errorMessage.value = strings.get(R.string.error_load_groups, strings.reason(it))
            }
            _isLoadingOptions.value = false
        }
    }

    private fun reportCoursesError(error: mitsoschedule.core.network.ApiError) {
        Log.e(tag, "Failed to load courses: $error")
        _errorMessage.value = strings.get(R.string.error_load_courses, strings.reason(error))
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
            store.saveSelection(updated)

            _pickerStep.value = WearPickerStep.FACULTY
            _currentScreen.value = WearScreen.SCHEDULE // Navigate back to schedule

            // список недель не критичен: расписание загрузится и без него
            val weekList = scheduleRepository.weeks(
                updated.facultyId,
                updated.formId.ifBlank { "Dnevnaya" },
                updated.courseId,
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
                    }
                }
                _isLoading.value = false
            }
            filterAndApplyDisplayedSchedule()
        }
        _currentScreen.value = WearScreen.SCHEDULE
    }

    private fun filterAndApplyDisplayedSchedule() {
        val all = _allScheduleData.value
        if (all.isEmpty()) {
            _scheduleData.value = emptyList()
            return
        }

        var selectedWeekId = _userSelection.value.weekId
        if (Weeks.isUnresolved(selectedWeekId)) {
            Weeks.resolveStored(_userSelection.value, _weeks.value, all, requireKnown = false)?.let { resolved ->
                selectedWeekId = resolved.weekId
                _userSelection.value = resolved
                viewModelScope.launch { store.saveSelection(resolved) }
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
            _errorMessage.value = strings.get(R.string.error_select_group)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = scheduleRepository.refresh(current, isManualRefresh)
            if (result.weeks.isNotEmpty()) {
                _weeks.value = result.weeks
            }

            if (result.days.isNotEmpty()) {
                _allScheduleData.value = result.days
                _userSelection.value = result.selection
                filterAndApplyDisplayedSchedule()
                _lastUpdateTime.value = result.updateTime
                onScheduleUpdated()
            } else if (_allScheduleData.value.isEmpty()) {
                // сбой сети и «расписание не опубликовано» это разные ситуации
                _errorMessage.value = result.error
                    ?.let { strings.get(R.string.error_schedule_load, strings.reason(it)) }
                    ?: strings.get(R.string.error_schedule_empty)
            }
            _isLoading.value = false
        }
    }

    // ----------------- Student Cabinet Methods -----------------

    fun loginStudent(login: String, pass: String) {
        viewModelScope.launch {
            _isStudentLoading.value = true
            _studentErrorMessage.value = null

            studentSession.login(login, pass, rememberMe = true)
                .onSuccess { data -> _studentCabinetData.value = data }
                .onFailure { exception ->
                    _studentErrorMessage.value = exception.message ?: strings.get(R.string.error_login_default)
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
}
