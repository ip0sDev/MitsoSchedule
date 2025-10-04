package by.iposdev.watchso.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import by.iposdev.watchso.data.WebWorker
import by.iposdev.watchso.presentation.theme.MitsoTestTheme
import by.iposdev.watchso.presentation.viewmodel.MainViewModel
import kotlinx.serialization.json.Json

val Context.scheduleDataStore: DataStore<Preferences> by preferencesDataStore(name = "schedule_cache")

data class UiDaySchedule(
    val dayTitle: String,
    val lessons: List<ParsedLessonEntry>
)

data class ParsedLessonEntry(
    val time: String?,
    val subject: String?,
    val room: String?
)

fun parseLessonEntryString(lessonEntry: String): ParsedLessonEntry {
    val timeRegex = Regex("^(\\d{2}\\.\\d{2}-\\d{2}\\.\\d{2})")
    val timeMatch = timeRegex.find(lessonEntry)
    val timeText = timeMatch?.value
    var remainingText = if (timeMatch != null) lessonEntry.substring(timeMatch.range.last + 1) else lessonEntry
    remainingText = remainingText.trim()
    if (remainingText.equals("(нет занятий)", ignoreCase = true)) {
        return ParsedLessonEntry(time = timeText, subject = "(нет занятий)", room = null)
    }
    val roomRegexes = listOf(
        Regex("(?:\\((?:каб|ауд)[.\\s]*([^)]+)\\)|(?:каб|ауд)[.\\s]*([^\\s]+))\$", RegexOption.IGNORE_CASE),
        Regex("\\b(\\d{1,3}[а-я]?)\\s*(?:каб|ауд)\$", RegexOption.IGNORE_CASE),
        Regex("\\b(\\d{1,3}[а-я]?)\$")
    )
    var roomText: String? = null
    var subjectText = remainingText
    for (regex in roomRegexes) {
        val roomMatch = regex.find(subjectText)
        if (roomMatch != null) {
            roomText = roomMatch.groups.drop(1).firstOrNull { it != null }?.value?.trim()
            if (roomText != null) {
                val roomRange = roomMatch.range
                subjectText = subjectText.substring(0, roomRange.first).trim() + subjectText.substring(roomRange.last + 1).trim()
                subjectText = subjectText.trim()
                break
            }
        }
    }
    subjectText = subjectText.replace("(нет занятий)", "", ignoreCase = true).trim()
    return ParsedLessonEntry(
        time = timeText?.trim(),
        subject = if (subjectText.isBlank() && timeText != null && !remainingText.equals("(нет занятий)", ignoreCase = true) ) "(описание отсутствует)" else subjectText.trim(),
        room = roomText?.trim()
    )
}

class MainActivity : ComponentActivity() {
    private val appJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private class MainViewModelFactory(
        private val dataStore: DataStore<Preferences>,
        private val json: Json,
        private val webWorker: WebWorker
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(dataStore, json, webWorker) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val webWorker = WebWorker(applicationContext)
        val viewModelFactory = MainViewModelFactory(applicationContext.scheduleDataStore, appJson, webWorker)
        setContent {
            WatchScheduleApp(viewModel = viewModel(factory = viewModelFactory))
        }
    }
}

@Composable
fun WatchScheduleApp(viewModel: MainViewModel = viewModel()) {
    MitsoTestTheme {
        val displaySchedule by viewModel.uiSchedule.collectAsState()
        val message by viewModel.message
        val isLoading by viewModel.isLoading
        val cacheInfo by viewModel.cacheInfoMessage.collectAsState()
        val listState = rememberScalingLazyListState()
        val context = LocalContext.current

        Scaffold(
            timeText = { TimeText(modifier = Modifier.padding(top = 6.dp)) }
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            } else if (displaySchedule.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(message, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { viewModel.fetchSchedule() }, colors = ButtonDefaults.buttonColors()) { Text("Загрузить") }
                    Chip(
                        onClick = { context.startActivity(Intent(context, AboutActivity::class.java)) },
                        label = { Text("О приложении") },
                        modifier = Modifier.padding(top = 10.dp),
                        colors = ChipDefaults.secondaryChipColors()
                    )
                    cacheInfo?.let {
                        Text(text = it, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    }
                }
            } else {
                ScalingLazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Chip(
                            onClick = { viewModel.fetchSchedule() },
                            label = { Text("Обновить") },
                            modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 4.dp)
                        )
                    }

                    displaySchedule.forEach { dayScheduleItem ->
                        item {
                            Text(
                                text = dayScheduleItem.dayTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp, top = 8.dp)
                            )
                        }
                        items(dayScheduleItem.lessons) { parsedLesson ->
                            Card(
                                onClick = { /* Static */ },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    parsedLesson.time?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.labelMedium,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    parsedLesson.subject?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodyLarge,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        )
                                    }
                                    parsedLesson.room?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.labelMedium,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(10.dp)) }
                    item {
                        Chip(
                            onClick = { context.startActivity(Intent(context, AboutActivity::class.java)) }, 
                            label = { Text("О приложении") },
                            modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 2.dp),
                            colors = ChipDefaults.secondaryChipColors()
                        )
                    }
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                    item {
                        cacheInfo?.let { info ->
                            Text(text = info, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
@Composable
fun WatchScheduleAppLoadedPreview() {
    val sampleDisplaySchedule = listOf(
        UiDaySchedule("Понедельник (Превью)", listOf(
            ParsedLessonEntry("08.00-09.20", "Супер Длинное Название Предмета Которое Должно Переноситься", "каб. 101"),
            ParsedLessonEntry("11.15-12.40", "Физкультура", "спортзал")
        )),
        UiDaySchedule("Вторник (Превью)", listOf(
            ParsedLessonEntry("09.35-11.00", "Важная Лекция", "Зал А"),
            ParsedLessonEntry(null, "Только предмет", null),
            ParsedLessonEntry("12.50-14.15", null, "ауд.303"),
            ParsedLessonEntry(null, null, "Только ауд.202")
        ))
    )
    val context = LocalContext.current

    MitsoTestTheme {
        Scaffold(timeText = { TimeText(modifier = Modifier.padding(top = 6.dp)) }) {
            ScalingLazyColumn(
                 modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 4.dp),
                 horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item { Chip(onClick = {}, label = { Text("Обновить (Превью)") }, modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 4.dp)) }
                
                sampleDisplaySchedule.forEach { dayScheduleItem ->
                    item {
                        Text(
                            text = dayScheduleItem.dayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp, top = 8.dp)
                        )
                    }
                    items(dayScheduleItem.lessons) { parsedLesson ->
                        Card(
                            onClick = { /* Static */ },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                parsedLesson.time?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.labelMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                parsedLesson.subject?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    )
                                }
                                parsedLesson.room?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.labelMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(10.dp)) }
                item { Chip(onClick = { context.startActivity(Intent(context, AboutActivity::class.java)) }, label = { Text("О приложении") }, modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 2.dp), colors = ChipDefaults.secondaryChipColors()) }
                item { Spacer(modifier = Modifier.height(4.dp)) }
                item { Text(text = "Кэш (Тест): 01.01.2024 12:00", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) }
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
@Composable
fun WatchScheduleAppEmptyPreview() {
    val context = LocalContext.current
    MitsoTestTheme {
         Scaffold(timeText = { TimeText(modifier = Modifier.padding(top = 6.dp)) }) {
            Column(
                modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Нет данных для отображения. Нажмите кнопку.", textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { /* Preview */ }) { Text("Загрузить") }
                Chip(onClick = { context.startActivity(Intent(context, AboutActivity::class.java)) }, label = { Text("О приложении") }, modifier = Modifier.padding(top = 10.dp), colors = ChipDefaults.secondaryChipColors())
                Text("Кеш не найден", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
@Composable
fun WatchScheduleAppLoadingPreview() {
    MitsoTestTheme {
        Scaffold(timeText = { TimeText(modifier = Modifier.padding(top = 6.dp)) }) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
        }
    }
}
