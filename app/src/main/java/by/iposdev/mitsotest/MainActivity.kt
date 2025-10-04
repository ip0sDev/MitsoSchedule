package by.iposdev.mitsotest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import by.iposdev.mitsotest.ui.theme.MitsoTestTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MitsoTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ScheduleScreen(
                        message = viewModel.message.value,
                        scheduleItems = viewModel.scheduleData.value,
                        onFetchClick = { viewModel.fetchSchedule() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleScreen(
    message: String,
    scheduleItems: List<DaySchedule>,
    onFetchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onFetchClick) {
            Text("Загрузить расписание")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (message.isNotEmpty()) {
            Text(text = message)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (scheduleItems.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(scheduleItems) { daySchedule ->
                    // Фильтруем занятия "(нет занятий)"
                    val lessonsToShow = daySchedule.lessons.filter { !it.text.contains("(нет занятий)") }

                    // Отображаем день, только если есть занятия для показа
                    if (lessonsToShow.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = daySchedule.dayTitle,
                                    style = MaterialTheme.typography.titleLarge, // Увеличил шрифт для заголовка дня
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                lessonsToShow.forEach { lesson ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Text(
                                            text = lesson.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(12.dp) // Увеличил внутренний отступ для текста занятия
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (message.isEmpty()) {
            Text(text = "Нет данных для отображения или все занятия отменены.")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScheduleScreenPreview() {
    MitsoTestTheme {
        val sampleSchedule = listOf(
            DaySchedule(
                "Понедельник, 29 сентября",
                listOf(
                    Lesson("08.00-9.25 (нет занятий)"), // Это занятие будет отфильтровано
                    Lesson("09.35-11.00 Важная лекция (лек) Профессор А.Б. 101"),
                    Lesson("11.10-12.35 (нет занятий)"),
                    Lesson("14.40-16.05 Белорусский язык (практ/сем) Ломака А. А. 31")
                )
            ),
            DaySchedule(
                "Вторник, 30 сентября",
                listOf(
                    Lesson("08.00-9.25 (нет занятий)"),
                    Lesson("09.35-11.00 Физическая культура(практическое) Преподаватель к."),
                    Lesson("14.40-16.05 (нет занятий)") // Это занятие будет отфильтровано
                )
            ),
            DaySchedule(
                "Среда, 01 октября", // Этот день не будет отображен, если все занятия "(нет занятий)"
                listOf(
                    Lesson("08.00-9.25 (нет занятий)"),
                    Lesson("09.35-11.00 (нет занятий)")
                )
            )
        )
        ScheduleScreen(
            message = "", // Убрал сообщение для чистоты превью данных
            scheduleItems = sampleSchedule,
            onFetchClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScheduleScreenEmptyDataPreview() {
    MitsoTestTheme {
        ScheduleScreen(
            message = "Все чисто!",
            scheduleItems = emptyList(),
            onFetchClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScheduleScreenLoadingPreview() {
    MitsoTestTheme {
        ScheduleScreen(
            message = "Загрузка расписания...",
            scheduleItems = emptyList(),
            onFetchClick = {}
        )
    }
}