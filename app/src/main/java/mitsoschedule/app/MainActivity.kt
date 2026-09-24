package mitsoschedule.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mitsoschedule.app.data.DaySchedule
import mitsoschedule.app.data.Lesson
import mitsoschedule.app.data.OptionItem
import mitsoschedule.app.data.StudentCabinetData
import mitsoschedule.app.data.UserSelection
import mitsoschedule.app.ui.components.AppFooter
import mitsoschedule.app.ui.components.DayScheduleSection
import mitsoschedule.app.ui.components.DayTimelineCategory
import mitsoschedule.app.ui.components.EmptyScheduleState
import mitsoschedule.app.ui.components.ErrorScheduleState
import mitsoschedule.app.ui.components.GroupHeaderCard
import mitsoschedule.app.ui.components.GroupSelectionBottomSheet
import androidx.compose.foundation.isSystemInDarkTheme
import mitsoschedule.app.ui.components.LoadingScheduleState
import mitsoschedule.app.ui.components.PastDaysAccordionCard
import mitsoschedule.app.ui.components.SettingsContent
import mitsoschedule.app.ui.components.StudentCabinetContent
import mitsoschedule.app.ui.components.StudentLoginCard
import mitsoschedule.app.ui.components.classifyDaySchedule
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import mitsoschedule.app.ui.haptics.LocalBiolumeHaptics
import mitsoschedule.app.ui.haptics.rememberBiolumeHaptics
import mitsoschedule.app.ui.theme.BiolumeTheme
import mitsoschedule.app.ui.theme.MitsoTestTheme
import mitsoschedule.app.ui.theme.biolumePressable
import mitsoschedule.app.ui.theme.biolumeRaised
import mitsoschedule.app.ui.theme.biolumeSurface

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode
            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            MitsoTestTheme(darkTheme = isDark) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncIfOlderThanWeek()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: MainViewModel
) {
    val haptics = rememberBiolumeHaptics()
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

    // Server & Settings State
    val serverHealth by viewModel.serverHealth
    val isCheckingHealth by viewModel.isCheckingHealth
    val themeMode by viewModel.themeMode

    var isBottomSheetOpen by remember { mutableStateOf(false) }
    var isPastDaysExpanded by remember { mutableStateOf(false) }

    // Haptic feedback triggers on data completion & error
    var hadLoadingStarted by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) {
        if (isLoading) {
            hadLoadingStarted = true
        } else if (hadLoadingStarted) {
            hadLoadingStarted = false
            if (scheduleData.isNotEmpty()) {
                haptics.success()
            }
        }
    }
    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            haptics.error()
        }
    }

    var hadStudentLoadingStarted by remember { mutableStateOf(false) }
    LaunchedEffect(isStudentLoading) {
        if (isStudentLoading) {
            hadStudentLoadingStarted = true
        } else if (hadStudentLoadingStarted) {
            hadStudentLoadingStarted = false
            if (studentCabinetData != null) {
                haptics.success()
            }
        }
    }
    LaunchedEffect(studentErrorMessage) {
        if (!studentErrorMessage.isNullOrBlank()) {
            haptics.error()
        }
    }

    CompositionLocalProvider(LocalBiolumeHaptics provides haptics) {

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
                                val topIcon = when (currentTab) {
                                    0 -> Icons.Outlined.School
                                    1 -> Icons.Outlined.Person
                                    else -> Icons.Outlined.Settings
                                }
                                Icon(
                                    imageVector = topIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        val titleText = when (currentTab) {
                            0 -> "МИТСО Расписание"
                            1 -> "Личный кабинет"
                            else -> "Настройки"
                        }
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    if (currentTab == 1 && studentCabinetData != null) {
                        IconButton(
                            onClick = {
                                haptics.click()
                                viewModel.refreshStudentCabinet()
                            },
                            enabled = !isStudentLoading
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Обновить кабинет",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (currentTab == 2) {
                        IconButton(
                            onClick = {
                                haptics.click()
                                viewModel.checkServerHealth()
                            },
                            enabled = !isCheckingHealth
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Проверить статус",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // Track previous tab for directional transitions
            var previousTab by remember { mutableIntStateOf(currentTab) }
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    val enter = slideInHorizontally { fullWidth -> direction * fullWidth / 4 } + fadeIn()
                    val exit = slideOutHorizontally { fullWidth -> -direction * fullWidth / 4 } + fadeOut()
                    (enter togetherWith exit).using(SizeTransform(clip = false))
                },
                modifier = Modifier.fillMaxSize()
            ) { tabIndex ->
            if (tabIndex == 0) {
                // SCHEDULE TAB
                val pastDays = remember(scheduleData) {
                    scheduleData.filter { classifyDaySchedule(it) == DayTimelineCategory.PAST }
                }
                val todayDays = remember(scheduleData) {
                    scheduleData.filter { classifyDaySchedule(it) == DayTimelineCategory.TODAY }
                }
                val futureDays = remember(scheduleData) {
                    scheduleData.filter { classifyDaySchedule(it) == DayTimelineCategory.FUTURE }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 110.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        GroupHeaderCard(
                            currentSelection = userSelection,
                            weeksList = weeks,
                            onOpenSelectionClick = {
                                haptics.mediumClick()
                                isBottomSheetOpen = true
                            },
                            onWeekSelected = {
                                haptics.tick()
                                viewModel.onWeekSelected(it)
                            },
                            canGoPrevious = viewModel.canGoPreviousWeek,
                            canGoNext = viewModel.canGoNextWeek,
                            onPreviousWeekClick = {
                                haptics.tick()
                                viewModel.selectPreviousWeek()
                            },
                            onNextWeekClick = {
                                haptics.tick()
                                viewModel.selectNextWeek()
                            }
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
                                onSelectGroupClick = {
                                    haptics.mediumClick()
                                    isBottomSheetOpen = true
                                }
                            )
                        }
                    } else if (!errorMessage.isNullOrBlank() && scheduleData.isEmpty()) {
                        item {
                            ErrorScheduleState(
                                errorMessage = errorMessage ?: "Произошла ошибка при загрузке расписания",
                                onRetryClick = {
                                    haptics.click()
                                    viewModel.fetchSchedule()
                                }
                            )
                        }
                    } else if (scheduleData.isEmpty()) {
                        item {
                            EmptyScheduleState(
                                message = "На выбранную неделю расписание занятий отсутствует.",
                                onSelectGroupClick = {
                                    haptics.mediumClick()
                                    isBottomSheetOpen = true
                                }
                            )
                        }
                    } else {
                        if (pastDays.isNotEmpty()) {
                            item {
                                PastDaysAccordionCard(
                                    pastDaysCount = pastDays.size,
                                    isExpanded = isPastDaysExpanded,
                                    onToggleExpand = {
                                        haptics.click()
                                        isPastDaysExpanded = !isPastDaysExpanded
                                    }
                                )
                            }

                            if (isPastDaysExpanded) {
                                items(pastDays) { daySchedule ->
                                    DayScheduleSection(daySchedule = daySchedule, isToday = false)
                                }
                            }
                        }

                        if (todayDays.isNotEmpty()) {
                            items(todayDays) { daySchedule ->
                                    DayScheduleSection(daySchedule = daySchedule, isToday = true)
                                }
                        }

                        items(futureDays) { daySchedule ->
                            DayScheduleSection(daySchedule = daySchedule, isToday = false)
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        AppFooter(lastUpdateTime = lastUpdateTime)
                    }
                }
            } else if (tabIndex == 1) {
                // STUDENT CABINET TAB
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 110.dp),
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
            } else {
                // SETTINGS TAB
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        SettingsContent(
                            serverHealth = serverHealth,
                            isCheckingHealth = isCheckingHealth,
                            lastUpdateTime = lastUpdateTime,
                            onCheckHealth = { viewModel.checkServerHealth() },
                            onClearCache = { viewModel.clearScheduleCache() },
                            onResetSelection = { viewModel.resetSelection() },
                            currentTheme = themeMode,
                            onThemeSelected = { viewModel.setThemeMode(it) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        AppFooter(lastUpdateTime = lastUpdateTime)
                    }
                }
            }
        }

        // Floating FAB (Schedule tab)
        if (currentTab == 0 && userSelection.isComplete && scheduleData.isNotEmpty()) {
            val fabDepth = BiolumeTheme.depth
            val fabShape = RoundedCornerShape(100.dp)
            val fabInteraction = remember { MutableInteractionSource() }
            ExtendedFloatingActionButton(
                onClick = {
                    haptics.click()
                    viewModel.fetchSchedule()
                },
                interactionSource = fabInteraction,
                icon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                text = { Text("Обновить") },
                shape = fabShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 24.dp, bottom = 96.dp)
                    .biolumeRaised(shape = fabShape, tokens = fabDepth)
                    .biolumePressable(
                        interactionSource = fabInteraction,
                        shape = fabShape,
                        tokens = fabDepth,
                        glowColor = fabDepth.glowPrimary
                    )
            )
        }

        // Floating Navigation Bar (hangs over content)
        val depth = BiolumeTheme.depth
        val navShape = RoundedCornerShape(28.dp)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            NavigationBar(
                modifier = Modifier
                    .clip(navShape)
                    .biolumeSurface(
                        shape = navShape,
                        tokens = depth,
                        containerColor = MaterialTheme.colorScheme.surface,
                        outlineColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = {
                        if (currentTab != 0) {
                            haptics.tick()
                            viewModel.selectTab(0)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 0) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = "Расписание"
                        )
                    },
                    label = { Text("Расписание", fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = depth.selectionFill
                    )
                )

                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = {
                        if (currentTab != 1) {
                            haptics.tick()
                            viewModel.selectTab(1)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 1) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Личный кабинет"
                        )
                    },
                    label = { Text("Кабинет", fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = depth.selectionFill
                    )
                )

                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = {
                        if (currentTab != 2) {
                            haptics.tick()
                            viewModel.selectTab(2)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Настройки"
                        )
                    },
                    label = { Text("Настройки", fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = depth.selectionFill
                    )
                )
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
                onApplySelection = {
                    haptics.mediumClick()
                    viewModel.applySelection(it)
                },
                onDismiss = { isBottomSheetOpen = false }
            )
        }
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