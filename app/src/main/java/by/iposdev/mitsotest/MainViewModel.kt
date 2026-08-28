package by.iposdev.mitsotest

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import by.iposdev.mitsotest.data.DaySchedule
import by.iposdev.mitsotest.data.OptionItem
import by.iposdev.mitsotest.data.PreferencesManager
import by.iposdev.mitsotest.data.StudentAuthCredentials
import by.iposdev.mitsotest.data.StudentCabinetData
import by.iposdev.mitsotest.data.StudentWebWorker
import by.iposdev.mitsotest.data.UserSelection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

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
            if (savedTime != null) {
                _lastUpdateTime.value = savedTime
            }

            if (saved != null && saved.isComplete) {
                _userSelection.value = saved
                loadDependentOptionsForSelection(saved)
                fetchSchedule()
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
                _weeks.value = weekList
            } catch (e: Exception) {
                // non-fatal
            }
        }
    }

    fun onWeekSelected(week: OptionItem) {
        _userSelection.value = _userSelection.value.copy(
            weekId = week.id,
            weekName = week.name
        )
        fetchSchedule()
    }

    fun applySelection(selection: UserSelection) {
        _userSelection.value = selection
        viewModelScope.launch {
            preferencesManager.saveSelection(selection)
            fetchSchedule()
        }
    }

    fun fetchSchedule() {
        val currentSelection = _userSelection.value
        if (!currentSelection.isComplete) {
            _errorMessage.value = "Сначала выберите группу"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = webWorker.fetchSchedule(currentSelection)
                _scheduleData.value = result

                if (result.isEmpty()) {
                    _errorMessage.value = "Занятия не найдены или расписание не опубликовано."
                } else {
                    val timeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    val formattedTime = timeFormat.format(Date())
                    _lastUpdateTime.value = formattedTime
                    preferencesManager.saveLastUpdateTime(formattedTime)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Не удалось загрузить расписание: ${e.message}"
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