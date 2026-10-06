package mitsoschedule.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll
import mitsoschedule.app.widget.ScheduleWidget
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import mitsoschedule.app.data.PreferencesManager
import mitsoschedule.core.network.StudentSession
import mitsoschedule.core.network.StudentWebWorker
import mitsoschedule.core.network.WebWorker
import mitsoschedule.core.schedule.ScheduleRepository
import mitsoschedule.core.ui.ContextUiStrings

/** Собирает зависимости приложения в одном месте; ViewModel получает их через конструктор. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val preferences = PreferencesManager(appContext)
    private val scheduleRepository = ScheduleRepository(WebWorker(), preferences)
    private val studentSession = StudentSession(StudentWebWorker(), preferences)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Обновляет кэш расписания без экрана приложения (кнопка на виджете).
     * Берёт сохранённую группу и адрес сервера из настроек; true, если пришли данные.
     */
    suspend fun refreshSchedule(): Boolean {
        val selection = preferences.getSavedSelection()?.takeIf { it.isComplete } ?: return false
        scheduleRepository.setServerUrl(preferences.serverUrlFlow.first())
        return scheduleRepository.refresh(selection, isManualRefresh = false).days.isNotEmpty()
    }

    val viewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            MainViewModel(
                scheduleRepository = scheduleRepository,
                studentSession = studentSession,
                store = preferences,
                settings = preferences,
                strings = ContextUiStrings(appContext),
                // свежее расписание сразу попадает на рабочий стол
                onScheduleUpdated = { scope.launch { ScheduleWidget().updateAll(appContext) } }
            )
        }
    }
}
