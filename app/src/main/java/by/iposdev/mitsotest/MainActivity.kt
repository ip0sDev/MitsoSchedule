package by.iposdev.mitsotest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import by.iposdev.mitsotest.data.DaySchedule
import by.iposdev.mitsotest.data.Lesson
import by.iposdev.mitsotest.data.OptionItem
import by.iposdev.mitsotest.data.StudentCabinetData
import by.iposdev.mitsotest.data.UserSelection
import by.iposdev.mitsotest.ui.components.AppFooter
import by.iposdev.mitsotest.ui.components.DayScheduleSection
import by.iposdev.mitsotest.ui.components.EmptyScheduleState
import by.iposdev.mitsotest.ui.components.ErrorScheduleState
import by.iposdev.mitsotest.ui.components.GroupHeaderCard
import by.iposdev.mitsotest.ui.components.GroupSelectionBottomSheet
import by.iposdev.mitsotest.ui.components.LoadingScheduleState
import by.iposdev.mitsotest.ui.components.StudentCabinetContent
import by.iposdev.mitsotest.ui.components.StudentLoginCard
import by.iposdev.mitsotest.ui.theme.MitsoTestTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MitsoTestTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: MainViewModel
) {
    val currentTab by viewModel.currentTab
    val userSelection by viewModel.userSelection
    val scheduleData by viewModel.scheduleData
    val faculties by viewModel.faculties
    val forms by viewModel.forms
    val courses by viewModel.courses
    val groups by viewModel.groups
    val weeks by viewModel.weeks
    val isLoading by viewModel.isLoading
    val isLoadingOptions by viewModel.isLoadingOptions
    val errorMessage by viewModel.errorMessage
    val lastUpdateTime by viewModel.lastUpdateTime

    // Student Cabinet State
    val studentCabinetData by viewModel.studentCabinetData
    val isStudentLoading by viewModel.isStudentLoading
    val studentErrorMessage by viewModel.studentErrorMessage

    var isBottomSheetOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (currentTab == 0) Icons.Outlined.School else Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (currentTab == 0) "МИТСО Расписание" else "Личный кабинет",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    if (currentTab == 0) {
                        IconButton(
                            onClick = { viewModel.fetchSchedule() },
                            enabled = userSelection.isComplete && !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Обновить расписание",
                                tint = if (userSelection.isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    } else if (studentCabinetData != null) {
                        IconButton(
                            onClick = { viewModel.refreshStudentCabinet() },
                            enabled = !isStudentLoading
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Обновить кабинет",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 0) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = "Расписание"
                        )
                    },
                    label = { Text("Расписание", fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 1) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Личный кабинет"
                        )
                    },
                    label = { Text("Кабинет", fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 0 && userSelection.isComplete && scheduleData.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.fetchSchedule() },
                    icon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                    text = { Text("Обновить") },
                    shape = RoundedCornerShape(100.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.padding(innerPadding)
        ) { tabIndex ->
            if (tabIndex == 0) {
                // SCHEDULE TAB
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        GroupHeaderCard(
                            currentSelection = userSelection,
                            weeksList = weeks,
                            onOpenSelectionClick = { isBottomSheetOpen = true },
                            onWeekSelected = { viewModel.onWeekSelected(it) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (isLoading && scheduleData.isEmpty()) {
                        item {
                            LoadingScheduleState()
                        }
                    } else if (!userSelection.isComplete) {
                        item {
                            EmptyScheduleState(
                                message = "Выберите ваш факультет, курс и группу, чтобы просмотреть актуальное расписание.",
                                onSelectGroupClick = { isBottomSheetOpen = true }
                            )
                        }
                    } else if (!errorMessage.isNullOrBlank() && scheduleData.isEmpty()) {
                        item {
                            ErrorScheduleState(
                                errorMessage = errorMessage ?: "Произошла ошибка при загрузке расписания",
                                onRetryClick = { viewModel.fetchSchedule() }
                            )
                        }
                    } else if (scheduleData.isEmpty()) {
                        item {
                            EmptyScheduleState(
                                message = "На выбранную неделю расписание занятий отсутствует.",
                                onSelectGroupClick = { isBottomSheetOpen = true }
                            )
                        }
                    } else {
                        items(scheduleData) { daySchedule ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically()
                            ) {
                                DayScheduleSection(daySchedule = daySchedule)
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        AppFooter(lastUpdateTime = lastUpdateTime)
                    }
                }
            } else {
                // STUDENT CABINET TAB
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (studentCabinetData != null) {
                        item {
                            StudentCabinetContent(
                                data = studentCabinetData!!,
                                isLoading = isStudentLoading,
                                onRefreshClick = { viewModel.refreshStudentCabinet() },
                                onLogoutClick = { viewModel.logoutStudent() }
                            )
                        }
                    } else {
                        item {
                            StudentLoginCard(
                                isLoading = isStudentLoading,
                                errorMessage = studentErrorMessage,
                                onLoginClick = { login, pass, rememberMe ->
                                    viewModel.loginStudent(login, pass, rememberMe)
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        AppFooter(lastUpdateTime = studentCabinetData?.lastFetchedTime)
                    }
                }
            }
        }

        // Modal Selection Bottom Sheet
        if (isBottomSheetOpen) {
            GroupSelectionBottomSheet(
                faculties = faculties,
                forms = forms,
                courses = courses,
                groups = groups,
                isLoadingOptions = isLoadingOptions,
                initialSelection = userSelection,
                onFacultyChanged = { viewModel.onFacultySelected(it) },
                onFormChanged = { viewModel.onFormSelected(it) },
                onCourseChanged = { viewModel.onCourseSelected(it) },
                onGroupChanged = { viewModel.onGroupSelected(it) },
                onApplySelection = { viewModel.applySelection(it) },
                onDismiss = { isBottomSheetOpen = false }
            )
        }
    }
}

// ----------------- Previews -----------------

@Preview(showBackground = true)
@Composable
fun SchedulePreviewLoaded() {
    MitsoTestTheme {
        val sampleSelection = UserSelection(
            facultyId = "YUridicheskij",
            facultyName = "Юридический",
            courseId = "1 kurs",
            courseName = "1 курс",
            groupId = "2631 PR",
            groupName = "2631 ПР",
            weekName = "31 августа - 06 сентября"
        )
        val sampleSchedule = listOf(
            DaySchedule(
                dayTitle = "Понедельник, 31 августа",
                lessons = listOf(
                    Lesson(time = "08.15 — 09.35", subject = "Гражданское право", type = "Лекция", room = "ауд. 312", teacher = "Иванов И.И."),
                    Lesson(time = "09.45 — 11.05", subject = "Уголовный процесс", type = "Практика / Семинар", room = "каб. 101", teacher = "Петров П.П."),
                    Lesson(time = "11.15 — 12.35", subject = "Физическая культура", type = "Практика / Семинар", room = "спортзал", teacher = "Сидоров С.С."),
                    Lesson(time = "14.40 — 16.05", subject = "Белорусский язык", type = "Практика / Семинар", room = "ауд. 51-52", teacher = "Ломака А. А.")
                )
            ),
            DaySchedule(
                dayTitle = "Вторник, 01 сентября",
                lessons = listOf(
                    Lesson(time = "09.45 — 11.05", subject = "Трудовое право", type = "Лекция", room = "ауд. 204", teacher = "Ковалева Е.А.")
                )
            )
        )

        Scaffold { padding ->
            LazyColumn(modifier = Modifier.padding(padding)) {
                item {
                    GroupHeaderCard(
                        currentSelection = sampleSelection,
                        weeksList = listOf(OptionItem("1", "31 августа - 06 сентября")),
                        onOpenSelectionClick = {},
                        onWeekSelected = {}
                    )
                }
                items(sampleSchedule) {
                    DayScheduleSection(daySchedule = it)
                }
                item {
                    AppFooter(lastUpdateTime = "28.08.2026 12:00")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StudentCabinetPreview() {
    MitsoTestTheme {
        val sampleData = StudentCabinetData(
            fullName = "Корзун Денис Алексеевич",
            accountDate = "28-08-2026",
            balance = "0.00",
            mainDebt = "0.00",
            penalty = "0.00",
            moodleGroup = "2423",
            moodleLogin = "419445",
            moodlePassword = "bbb01937",
            lastFetchedTime = "28.08.2026 13:00"
        )

        Scaffold { padding ->
            LazyColumn(modifier = Modifier.padding(padding)) {
                item {
                    StudentCabinetContent(
                        data = sampleData,
                        isLoading = false,
                        onRefreshClick = {},
                        onLogoutClick = {}
                    )
                }
                item {
                    AppFooter(lastUpdateTime = sampleData.lastFetchedTime)
                }
            }
        }
    }
}