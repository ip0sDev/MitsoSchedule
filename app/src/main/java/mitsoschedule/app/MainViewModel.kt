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

    private var savedCredentials: StudentAuthCredentials? = null

    init {
        loadInitialData()
        loadInitialStudentData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            loadFaculties()

            val saved = preferencesManager.savedSelectionFlow.firstOrNull()
            val savedTime = preferencesManager.lastUpdateFlow.firstOrNull()
            val cachedSchedule = preferencesManager.cachedScheduleFlow.firstOrNull()
            val lastFetchMillis = preferencesManager.lastFetchMillisFlow.firstOrNull() ?: 0L

            if (savedTime != null) {
                _lastUpdateTime.value = savedTime
            }

            if (cachedSchedule != null && cachedSchedule.isNotEmpty()) {
                _allScheduleData.value = cachedSchedule

                // Normalize the saved week id against cached data (old saves may have "0")
                if (saved != null) {
                    val knownWeekIds = cachedSchedule.map { it.weekId }.filter { it.isNotBlank() }.distinct()
                    val savedWeekId = saved.weekId
                    if (savedWeekId.isNotBlank() && savedWeekId != ALL_WEEKS_ID && savedWeekId !in knownWeekIds && knownWeekIds.isNotEmpty()) {
                        val firstId = knownWeekIds.first()
                        val firstName = cachedSchedule.firstOrNull { it.weekId == firstId }?.weekName ?: saved.weekName
                        _userSelection.value = saved.copy(weekId = firstId, weekName = firstName)
                    }
                }
                filterAndApplyDisplayedSchedule()
            }

            if (saved != null && saved.isComplete) {
                _userSelection.value = saved
                loadDependentOptionsForSelection(saved)

                val shouldRefresh = PreferencesManager.shouldAutoRefresh(lastFetchMillis)
                if (shouldRefresh || cachedSchedule.isNullOrEmpty()) {
                    fetchSchedule(isManualRefresh = false)
                }
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
                groupName = group.name
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
            if (_allScheduleData.value.isNotEmpty()) {
                filterAndApplyDisplayedSchedule()
            } else {
                fetchSchedule(isManualRefresh = true)
            }
        }
    }

    private fun filterAndApplyDisplayedSchedule() {
        val selectedWeekId = _userSelection.value.weekId
        val all = _allScheduleData.value
        if (selectedWeekId == ALL_WEEKS_ID) {
            _scheduleData.value = all
        } else {
            // Filter strictly by week: if the selected week has no data, show empty state
            _scheduleData.value = all.filter { it.weekId == selectedWeekId }
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

                    // Re-map the saved week id (e.g. default "0") to a real server week id
                    // so navigation/filtering work. "ALL" stays as is.
                    val current = _userSelection.value
                    if (current.weekId != ALL_WEEKS_ID) {
                        val resolved = result.weeks.find { it.id == current.weekId } ?: result.weeks.firstOrNull()
                        if (resolved != null && resolved.id != current.weekId) {
                            val updated = current.copy(weekId = resolved.id, weekName = resolved.name)
                            _userSelection.value = updated
                            preferencesManager.saveSelection(updated)
                        } else if (resolved != null && resolved.name != current.weekName) {
                            val updated = current.copy(weekName = resolved.name)
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
}