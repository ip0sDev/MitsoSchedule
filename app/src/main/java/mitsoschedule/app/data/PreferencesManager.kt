package mitsoschedule.app.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mitsoschedule.core.network.ServerConfig
import mitsoschedule.core.storage.ScheduleStore
import mitsoschedule.core.storage.keystoreSecretCipher

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mitso_schedule_prefs")

/** Настройки, которые есть только у телефона. Интерфейс нужен, чтобы ViewModel тестировался без Context. */
interface AppSettings {
    val serverUrlFlow: Flow<String>
    val themeModeFlow: Flow<String>
    val dynamicColorFlow: Flow<Boolean>
    suspend fun saveServerUrl(url: String)
    suspend fun setThemeMode(mode: String)
    suspend fun setDynamicColor(enabled: Boolean)
}

/** Общее хранилище расписания плюс настройки, которые есть только у телефона: сервер и тема. */
class PreferencesManager(context: Context) : ScheduleStore(
    dataStore = context.applicationContext.dataStore,
    keyPrefix = "",
    cipher = keystoreSecretCipher("mitso_student_secrets")
), AppSettings {

    companion object {
        const val DEFAULT_SERVER_URL = ServerConfig.DEFAULT_SERVER_URL
        private val KEY_SERVER_URL = stringPreferencesKey("server_base_url")
        private val KEY_THEME_MODE = stringPreferencesKey("app_theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("app_dynamic_color")
    }

    override val serverUrlFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_SERVER_URL]?.takeIf { it.isNotBlank() } ?: DEFAULT_SERVER_URL
    }

    override suspend fun saveServerUrl(url: String) {
        dataStore.edit { it[KEY_SERVER_URL] = url.trimEnd('/') }
    }

    override val themeModeFlow: Flow<String> = dataStore.data.map { it[KEY_THEME_MODE] ?: "system" }

    override suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    override val dynamicColorFlow: Flow<Boolean> = dataStore.data.map { it[KEY_DYNAMIC_COLOR] ?: false }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }
}
