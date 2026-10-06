package by.iposdev.watchso

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import by.iposdev.watchso.complication.MainComplicationService
import by.iposdev.watchso.data.WatchPreferencesManager
import by.iposdev.watchso.data.createWatchStudentWebWorker
import by.iposdev.watchso.data.createWatchWebWorker
import by.iposdev.watchso.presentation.viewmodel.MainViewModel
import by.iposdev.watchso.tile.ScheduleTileService
import mitsoschedule.core.network.StudentSession
import mitsoschedule.core.schedule.ScheduleRepository
import mitsoschedule.core.ui.ContextUiStrings

/** Собирает зависимости часов в одном месте; ViewModel получает их через конструктор. */
class WatchContainer(application: Application) {
    private val preferences = WatchPreferencesManager(application)
    private val scheduleRepository =
        ScheduleRepository(createWatchWebWorker(), preferences, updateTimePattern = "dd.MM HH:mm")
    private val studentSession = StudentSession(createWatchStudentWebWorker(), preferences)

    val viewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            MainViewModel(
                scheduleRepository = scheduleRepository,
                studentSession = studentSession,
                store = preferences,
                strings = ContextUiStrings(application),
                onScheduleUpdated = { notifyTileAndComplications(application) }
            )
        }
    }

    private fun notifyTileAndComplications(app: Application) {
        try {
            TileService.getUpdater(app).requestUpdate(ScheduleTileService::class.java)
            ComplicationDataSourceUpdateRequester.create(
                app,
                ComponentName(app, MainComplicationService::class.java)
            ).requestUpdateAll()
        } catch (_: Exception) {
            // плитка и компликации обновятся при следующем запросе системы
        }
    }
}
