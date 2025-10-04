package by.iposdev.mitsotest

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    // Состояние для данных расписания
    val scheduleData = mutableStateOf<List<DaySchedule>>(emptyList())
    // Состояние для сообщений UI (статус, ошибки)
    val message = mutableStateOf("Нажмите кнопку, чтобы загрузить расписание")

    private val webWorker = WebWorker()

    fun fetchSchedule() {
        viewModelScope.launch {
            message.value = "Загрузка..."
            scheduleData.value = emptyList() // Очищаем предыдущие данные
            try {
                val result = webWorker.sendRequest()
                if (result.isEmpty()) {
                    // Это может означать, что токен не получен, запрос не удался, или данные не распарсились
                    // WebWorker уже залогировал конкретную причину
                    message.value = "Расписание не найдено или не удалось загрузить."
                } else {
                    scheduleData.value = result
                    message.value = "" // Очищаем сообщение, если данные успешно загружены
                }
            } catch (e: Exception) {
                // Эта ошибка здесь маловероятна, так как WebWorker обрабатывает свои исключения
                // и возвращает emptyList() в случае проблем. Но оставим на всякий случай.
                message.value = "Ошибка при выполнении запроса: ${e.message}"
                scheduleData.value = emptyList()
            }
        }
    }
}