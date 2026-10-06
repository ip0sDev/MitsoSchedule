package by.iposdev.watchso.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import mitsoschedule.core.storage.ScheduleStore
import mitsoschedule.core.storage.keystoreSecretCipher

val Context.watchDataStore: DataStore<Preferences> by preferencesDataStore(name = "mitso_watch_prefs")

/** Хранилище часов: те же данные, что и на телефоне, ключи с префиксом `watch_`. */
class WatchPreferencesManager(context: Context) : ScheduleStore(
    dataStore = context.applicationContext.watchDataStore,
    keyPrefix = "watch_",
    cipher = keystoreSecretCipher("mitso_watch_student_secrets")
)
