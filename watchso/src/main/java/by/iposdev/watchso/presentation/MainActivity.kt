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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import by.iposdev.watchso.BuildConfig
import by.iposdev.watchso.data.DaySchedule
import by.iposdev.watchso.data.Lesson
import by.iposdev.watchso.data.StudentCabinetData
import by.iposdev.watchso.data.UserSelection
import by.iposdev.watchso.presentation.components.WearDayHeader
import by.iposdev.watchso.presentation.components.WearGroupHeaderCard
import by.iposdev.watchso.presentation.components.WearGroupPickerContent
import by.iposdev.watchso.presentation.components.WearLessonCard
import by.iposdev.watchso.presentation.components.WearNavHeader
import by.iposdev.watchso.presentation.components.WearStudentCabinetContent
import by.iposdev.watchso.presentation.components.WearStudentLoginCard
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

    val listState = rememberScalingLazyListState()
    val context = LocalContext.current

    Scaffold(
        timeText = { TimeText(modifier = Modifier.padding(top = 4.dp)) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Top Navigation Bar (Пары | Кабинет | Группа)
            item(key = "top_nav_header") {
                Spacer(modifier = Modifier.height(18.dp))
                WearNavHeader(
                    currentScreen = currentScreen,
                    onNavigate = { viewModel.navigateTo(it) }
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
                            onEditClick = { viewModel.navigateTo(2) }
                        )
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
                            Text(
                                text = "Группа не выбрана.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Chip(
                                onClick = { viewModel.navigateTo(2) },
                                label = { Text("Выбрать группу") },
                                icon = { Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) },
                                colors = ChipDefaults.primaryChipColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
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
                                    onClick = { viewModel.fetchSchedule(isManualRefresh = true) },
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
                                    onClick = { viewModel.fetchSchedule(isManualRefresh = true) },
                                    label = { Text("Обновить", fontSize = 11.sp) },
                                    icon = { Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) },
                                    colors = ChipDefaults.primaryChipColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        scheduleData.forEach { daySchedule ->
                            item(key = "day_hdr_${daySchedule.dayTitle}") {
                                WearDayHeader(dayTitle = daySchedule.dayTitle)
                            }
                            items(
                                items = daySchedule.lessons,
                                key = { lesson -> "lesson_${daySchedule.dayTitle}_${lesson.time}_${lesson.subject}_${lesson.room}" }
                            ) { lesson ->
                                WearLessonCard(lesson = lesson)
                            }
                        }

                        // Bottom actions only when schedule data is displayed
                        item(key = "schedule_refresh_chip") {
                            Spacer(modifier = Modifier.height(6.dp))
                            Chip(
                                onClick = { viewModel.fetchSchedule(isManualRefresh = true) },
                                label = { Text("Обновить расписание", fontSize = 11.sp) },
                                icon = { Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) },
                                colors = ChipDefaults.primaryChipColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item(key = "schedule_about_chip") {
                        Chip(
                            onClick = { context.startActivity(Intent(context, AboutActivity::class.java)) },
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
                                onRefreshClick = { viewModel.refreshStudentCabinet() },
                                onLogoutClick = { viewModel.logoutStudent() }
                            )
                        }
                    } else {
                        item(key = "cabinet_login_card") {
                            WearStudentLoginCard(
                                isLoading = isStudentLoading,
                                errorMessage = studentError,
                                onLoginClick = { login, pass ->
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
                    item(key = "group_picker_content") {
                        WearGroupPickerContent(
                            faculties = faculties,
                            forms = forms,
                            courses = courses,
                            groups = groups,
                            weeks = weeks,
                            currentSelection = userSelection,
                            isLoading = isLoadingOptions,
                            onFacultySelected = { viewModel.onFacultySelected(it) },
                            onFormSelected = { viewModel.onFormSelected(it) },
                            onCourseSelected = { viewModel.onCourseSelected(it) },
                            onGroupSelected = { viewModel.onGroupSelected(it) },
                            onWeekSelected = { viewModel.onWeekSelected(it) },
                            onBackToSchedule = { viewModel.navigateTo(0) }
                        )
                    }
                    item(key = "picker_bottom_spacer") {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
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
                    item { WearDayHeader(dayTitle = day.dayTitle) }
                    items(day.lessons) { lesson ->
                        WearLessonCard(lesson = lesson)
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
