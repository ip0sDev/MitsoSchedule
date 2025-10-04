package by.iposdev.watchso.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.iposdev.watchso.data.DaySchedule
import by.iposdev.watchso.data.WebWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScheduleViewModel(
    private val webWorker: WebWorker // WebWorker передается через конструктор
) : ViewModel() {

    // private val webWorker = WebWorker() // Удалено локальное создание

    private val _scheduleState = MutableStateFlow<List<DaySchedule>>(emptyList())
    val scheduleState: StateFlow<List<DaySchedule>> = _scheduleState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    init {
        // Загрузка расписания при инициализации ViewModel
        loadSchedule()
    }

    fun loadSchedule() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorState.value = null // Сброс предыдущей ошибки
            Log.d("ScheduleViewModel", "Starting to load schedule...")
            try {
                val result = webWorker.sendRequest() // Теперь используется webWorker из конструктора
                Log.d("ScheduleViewModel", "ViewModel received ${result.size} days from WebWorker")
                _scheduleState.value = result
                if (result.isEmpty()) {
                     Log.w("ScheduleViewModel", "WebWorker returned empty list. Data might be parsed as 0 days or actual schedule is empty.")
                    // Можно установить специфичное состояние "нет данных" или информационное сообщение
                    // _errorState.value = "Нет данных о расписании или расписание на сегодня отсутствует." // Опционально, если хотим показать это как ошибку
                }
            } catch (e: Exception) {
                Log.e("ScheduleViewModel", "Error loading schedule from WebWorker", e)
                _errorState.value = "Ошибка загрузки: ${e.localizedMessage ?: "Неизвестная ошибка"}"
                _scheduleState.value = emptyList() // Очищаем данные в случае ошибки
            } finally {
                _isLoading.value = false
                Log.d("ScheduleViewModel", "Finished loading schedule. Loading: ${_isLoading.value}, Days: ${_scheduleState.value.size}, Error: ${_errorState.value}")
            }
        }
    }
}