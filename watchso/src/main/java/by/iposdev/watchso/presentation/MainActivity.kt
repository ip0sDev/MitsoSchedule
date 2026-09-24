package by.iposdev.watchso.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.PaddingValues
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.scrollAway
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import by.iposdev.watchso.BuildConfig
import by.iposdev.watchso.data.DaySchedule
import by.iposdev.watchso.data.Lesson
import by.iposdev.watchso.data.ScheduleTimeUtils
import by.iposdev.watchso.data.StudentCabinetData
import by.iposdev.watchso.data.UserSelection
import by.iposdev.watchso.presentation.components.WearDayHeader
import by.iposdev.watchso.presentation.components.WearGroupHeaderCard
import by.iposdev.watchso.presentation.components.wearGroupPickerItems
import by.iposdev.watchso.presentation.components.WearLessonCard
import by.iposdev.watchso.presentation.components.WearNavHeader
import by.iposdev.watchso.presentation.components.WearPullToRefreshIndicator
import by.iposdev.watchso.presentation.components.WearStudentCabinetContent
import by.iposdev.watchso.presentation.components.WearStudentLoginCard
import by.iposdev.watchso.presentation.components.WearTodayStatusBanner
import by.iposdev.watchso.presentation.components.WearWeekNavigator
import by.iposdev.watchso.presentation.haptics.rememberWearBiolumeHaptics
import by.iposdev.watchso.presentation.theme.MitsoTestTheme
import by.iposdev.watchso.presentation.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            MitsoTestTheme {
                WatchMainApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncIfOlderThanWeek()
    }
}

@Composable
fun WatchMainApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen
    val userSelection by viewModel.userSelection
    val scheduleData by viewModel.scheduleData
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    val lastUpdateTime by viewModel.lastUpdateTime

    // Student state
    val studentData by viewModel.studentCabinetData
    val isStudentLoading by viewModel.isStudentLoading
    val studentError by viewModel.studentErrorMessage

    // Picker state
    val faculties by viewModel.faculties
    val forms by viewModel.forms
    val courses by viewModel.courses
    val groups by viewModel.groups
    val weeks by viewModel.weeks
    val isLoadingOptions by viewModel.isLoadingOptions
    val pickerStep by viewModel.pickerStep

    val todaySchedule = remember(scheduleData) { ScheduleTimeUtils.findTodaySchedule(scheduleData) }
    val todayTimeInfo = remember(todaySchedule) {
        todaySchedule?.let { ScheduleTimeUtils.calculateTodayTimeInfo(it.lessons) }
    }

    val listState = rememberScalingLazyListState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val wearHaptics = rememberWearBiolumeHaptics()

    var wasLoading by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading, errorMessage) {
        if (wasLoading && !isLoading) {
            if (errorMessage == null) {
                wearHaptics.success()
            } else {
                wearHaptics.error()
            }
        }
        wasLoading = isLoading
    }

    var wasStudentLoading by remember { mutableStateOf(false) }
    LaunchedEffect(isStudentLoading, studentError) {
        if (wasStudentLoading && !isStudentLoading) {
            if (studentError == null) {
                wearHaptics.success()
            } else {
                wearHaptics.error()
            }
        }
        wasStudentLoading = isStudentLoading
    }

    var pullOffset by remember { mutableFloatStateOf(0f) }
    var hasTriggeredSnap by remember { mutableStateOf(false) }
    val pullThresholdPx = remember(density) { with(density) { 52.dp.toPx() } }
    val maxPullPx = remember(density) { with(density) { 76.dp.toPx() } }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If user is dragging upward while refresh indicator is pulled, collapse it first
                if (pullOffset > 0f && available.y < 0f) {
                    val newOffset = (pullOffset + available.y).coerceAtLeast(0f)
                    val consumedY = newOffset - pullOffset
                    pullOffset = newOffset
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Only accumulate on deliberate touch drag when user is at the very top (never on fling inertia)
                if (currentScreen == 0 && !isLoading && source == NestedScrollSource.Drag && available.y > 0f && listState.centerItemIndex <= 1) {
                    val newOffset = (pullOffset + available.y * 0.4f).coerceAtMost(maxPullPx)
                    val consumedY = newOffset - pullOffset
                    pullOffset = newOffset
                    if (newOffset >= pullThresholdPx && !hasTriggeredSnap) {
                        wearHaptics.snap()
                        hasTriggeredSnap = true
                    } else if (newOffset < pullThresholdPx && hasTriggeredSnap) {
                        hasTriggeredSnap = false
                    }
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffset >= pullThresholdPx && !isLoading) {
                    wearHaptics.click()
                    pullOffset = 0f
                    hasTriggeredSnap = false
                    viewModel.fetchSchedule(isManualRefresh = true)
                } else {
                    pullOffset = 0f
                    hasTriggeredSnap = false
                }
                return Velocity.Zero
            }
        }
    }

    Scaffold(
        timeText = { TimeText(modifier = Modifier.scrollAway(scrollState = listState)) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            ScalingLazyColumn(
                state = listState,
                autoCentering = null,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 8.dp, top = 36.dp, end = 8.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Top Navigation Bar (Пары | Кабинет | Группа)
                item(key = "top_nav_header") {
                    WearNavHeader(
                        currentScreen = currentScreen,
                        onNavigate = {
                            wearHaptics.tick()
                            viewModel.navigateTo(it)
                        }
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

            // 2. Active Screen Content
            when (currentScreen) {
                0 -> {
                    // --- SCHEDULE SCREEN ---
                    item(key = "schedule_group_header") {
                        WearGroupHeaderCard(
                            userSelection = userSelection,
                            onEditClick = {
                                wearHaptics.click()
                                viewModel.navigateTo(2)
                            },
                            onRefreshClick = {
                                wearHaptics.click()
                                viewModel.fetchSchedule(isManualRefresh = true)
                            },
                            isLoading = isLoading
                        )
                    }

                    if (userSelection.isComplete) {
                        item(key = "schedule_week_nav") {
                            WearWeekNavigator(
                                weekName = userSelection.weekName.ifBlank { "Текущая неделя" },
                                canGoPrev = viewModel.canGoPreviousWeek,
                                canGoNext = viewModel.canGoNextWeek,
                                onPrevClick = {
                                    wearHaptics.tick()
                                    viewModel.selectPreviousWeek()
                                },
                                onNextClick = {
                                    wearHaptics.tick()
                                    viewModel.selectNextWeek()
                                }
                            )
                        }
                    }

                    if (todayTimeInfo != null && todayTimeInfo.state != by.iposdev.watchso.data.TodayScheduleState.NO_LESSONS) {
                        item(key = "schedule_today_banner") {
                            WearTodayStatusBanner(todayInfo = todayTimeInfo)
                        }
                    }

                    if (isLoading && scheduleData.isEmpty()) {
                        item(key = "schedule_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    } else if (!userSelection.isComplete) {
                        item(key = "schedule_no_selection") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Группа не выбрана.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Chip(
                                    onClick = {
                                        wearHaptics.click()
                                        viewModel.navigateTo(2)
                                    },
                                    label = { Text("Выбрать группу") },
                                    icon = { Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) },
                                    colors = ChipDefaults.primaryChipColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else if (!errorMessage.isNullOrBlank() && scheduleData.isEmpty()) {
                        item(key = "schedule_error") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = errorMessage ?: "Ошибка загрузки",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Chip(
                                    onClick = {
                                        wearHaptics.click()
                                        viewModel.fetchSchedule(isManualRefresh = true)
                                    },
                                    label = { Text("Повторить", fontSize = 11.sp) },
                                    icon = { Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) },
                                    colors = ChipDefaults.primaryChipColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else if (scheduleData.isEmpty()) {
                        item(key = "schedule_empty_week") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "На выбранную неделю пар нет.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Chip(
                                    onClick = {
                                        wearHaptics.click()
                                        viewModel.fetchSchedule(isManualRefresh = true)
                                    },
                                    enabled = !isLoading,
                                    label = { Text(if (isLoading) "Обновление..." else "Обновить", fontSize = 11.sp) },
                                    icon = {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(ChipDefaults.IconSize),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize))
                                        }
                                    },
                                    colors = ChipDefaults.primaryChipColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        scheduleData.forEach { daySchedule ->
                            val isToday = (daySchedule == todaySchedule)
                            item(key = "day_hdr_${daySchedule.dayTitle}") {
                                WearDayHeader(dayTitle = daySchedule.dayTitle, isToday = isToday)
                            }
                            items(
                                items = daySchedule.lessons,
                                key = { lesson -> "lesson_${daySchedule.dayTitle}_${lesson.time}_${lesson.subject}_${lesson.room}" }
                            ) { lesson ->
                                val isCurrent = isToday && (todayTimeInfo?.currentLesson == lesson)
                                WearLessonCard(lesson = lesson, isCurrent = isCurrent)
                            }
                        }

                        // Bottom actions only when schedule data is displayed
                        item(key = "schedule_refresh_chip") {
                            Spacer(modifier = Modifier.height(6.dp))
                            Chip(
                                onClick = {
                                    wearHaptics.click()
                                    viewModel.fetchSchedule(isManualRefresh = true)
                                },
                                enabled = !isLoading,
                                label = { Text(if (isLoading) "Обновление..." else "Обновить расписание", fontSize = 11.sp) },
                                icon = {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(ChipDefaults.IconSize),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize))
                                    }
                                },
                                colors = ChipDefaults.primaryChipColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item(key = "schedule_about_chip") {
                        Chip(
                            onClick = {
                                wearHaptics.click()
                                context.startActivity(Intent(context, AboutActivity::class.java))
                            },
                            label = { Text("О приложении", fontSize = 11.sp) },
                            icon = { Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) },
                            colors = ChipDefaults.secondaryChipColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item(key = "schedule_version_footer") {
                        val versionStr = "v${BuildConfig.VERSION_NAME}"
                        val cacheStr = lastUpdateTime?.let { " • $it" } ?: ""
                        Text(
                            text = "МИТСО $versionStr$cacheStr",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                1 -> {
                    // --- STUDENT CABINET SCREEN ---
                    if (studentData != null) {
                        item(key = "cabinet_content") {
                            WearStudentCabinetContent(
                                data = studentData!!,
                                isLoading = isStudentLoading,
                                onRefreshClick = {
                                    wearHaptics.click()
                                    viewModel.refreshStudentCabinet()
                                },
                                onLogoutClick = {
                                    wearHaptics.click()
                                    viewModel.logoutStudent()
                                }
                            )
                        }
                    } else {
                        item(key = "cabinet_login_card") {
                            WearStudentLoginCard(
                                isLoading = isStudentLoading,
                                errorMessage = studentError,
                                onLoginClick = { login, pass ->
                                    wearHaptics.click()
                                    viewModel.loginStudent(login, pass)
                                }
                            )
                        }
                    }

                    item(key = "cabinet_bottom_spacer") {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                2 -> {
                    // --- GROUP PICKER SCREEN ---
                    wearGroupPickerItems(
                        step = pickerStep,
                        faculties = faculties,
                        courses = courses,
                        groups = groups,
                        currentSelection = userSelection,
                        isLoading = isLoadingOptions,
                        onFacultySelected = {
                            wearHaptics.tick()
                            viewModel.onFacultySelected(it)
                        },
                        onCourseSelected = {
                            wearHaptics.tick()
                            viewModel.onCourseSelected(it)
                        },
                        onGroupSelected = {
                            wearHaptics.click()
                            viewModel.onGroupSelected(it)
                        },
                        onStepChange = {
                            wearHaptics.tick()
                            viewModel.setPickerStep(it)
                        },
                        onRetryFaculties = {
                            wearHaptics.click()
                            viewModel.loadFaculties()
                        },
                        onBackToSchedule = {
                            wearHaptics.click()
                            viewModel.navigateTo(0)
                        }
                    )
                    item(key = "picker_bottom_spacer") {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }

        // Pull to refresh overlay indicator
        if (pullOffset > 0f || isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                WearPullToRefreshIndicator(
                    pullOffset = pullOffset,
                    pullThreshold = pullThresholdPx,
                    isRefreshing = isLoading
                )
            }
        }
    }
}
}

// ----------------- Previews -----------------

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
@Composable
fun WatchScheduleAppLoadedPreview() {
    val sampleSelection = UserSelection(
        facultyId = "1",
        facultyName = "Юридический",
        courseId = "1",
        courseName = "1 курс",
        groupId = "2631",
        groupName = "2631 ПР"
    )
    val sampleSchedule = listOf(
        DaySchedule(
            dayTitle = "Понедельник, 31 августа",
            lessons = listOf(
                Lesson(time = "08.15 — 09.35", subject = "Гражданское право", type = "Лекция", room = "ауд. 312", teacher = "Иванов И. И."),
                Lesson(time = "09.45 — 11.05", subject = "Уголовный процесс", type = "Практика / Семинар", room = "каб. 101", teacher = "Петров П. П."),
                Lesson(time = "14.40 — 16.05", subject = "Белорусский язык", type = "Практика / Семинар", room = "ауд. 51-52", teacher = "Ломака А. А.")
            )
        )
    )

    MitsoTestTheme {
        Scaffold(timeText = { TimeText(modifier = Modifier.padding(top = 4.dp)) }) {
            ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    WearNavHeader(currentScreen = 0, onNavigate = {})
                }
                item {
                    WearGroupHeaderCard(userSelection = sampleSelection, onEditClick = {})
                }
                sampleSchedule.forEach { day ->
                    item { WearDayHeader(dayTitle = day.dayTitle, isToday = true) }
                    items(day.lessons) { lesson ->
                        WearLessonCard(lesson = lesson, isCurrent = (lesson == day.lessons[1]))
                    }
                }
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
@Composable
fun WatchStudentCabinetPreview() {
    val sampleData = StudentCabinetData(
        fullName = "Корзун Денис Алексеевич",
        accountDate = "28-08-2026",
        balance = "0.00",
        mainDebt = "0.00",
        penalty = "0.00",
        moodleGroup = "2423",
        moodleLogin = "419445",
        moodlePassword = "bbb01937"
    )

    MitsoTestTheme {
        Scaffold(timeText = { TimeText(modifier = Modifier.padding(top = 4.dp)) }) {
            ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    WearNavHeader(currentScreen = 1, onNavigate = {})
                }
                item {
                    WearStudentCabinetContent(
                        data = sampleData,
                        isLoading = false,
                        onRefreshClick = {},
                        onLogoutClick = {}
                    )
                }
            }
        }
    }
}
