package mitsoschedule.core.network

import kotlinx.coroutines.flow.firstOrNull
import mitsoschedule.core.model.StudentAuthCredentials
import mitsoschedule.core.model.StudentCabinetData
import mitsoschedule.core.storage.ScheduleStore

/** Сессия кабинета студента: вход, обновление и выход с сохранением в [store]. */
open class StudentSession(
    private val api: StudentWebWorker,
    private val store: ScheduleStore
) {
    private var credentials: StudentAuthCredentials? = null

    open val isSignedIn: Boolean get() = credentials != null

    open fun setServerUrl(url: String) = api.updateBaseUrl(url)

    /** Шифрует значения, оставшиеся от старых версий открытым текстом. Вызывать до чтения. */
    open suspend fun migrateLegacySecrets() = store.migrateLegacySecrets()

    open suspend fun cachedData(): StudentCabinetData? = store.cachedStudentCabinetFlow.firstOrNull()

    /**
     * Восстанавливает сохранённые учётные данные. С [requireRemember] подходят только те,
     * что пользователь разрешил запомнить.
     */
    open suspend fun restore(requireRemember: Boolean): StudentAuthCredentials? {
        val saved = store.studentCredentialsFlow.firstOrNull()
            ?.takeIf { it.login.isNotBlank() && (!requireRemember || it.rememberMe) }
        credentials = saved
        return saved
    }

    open suspend fun login(login: String, password: String, rememberMe: Boolean): Result<StudentCabinetData> =
        api.loginAndFetch(login, password).onSuccess { data ->
            val creds = StudentAuthCredentials(login, password, rememberMe)
            credentials = creds
            if (rememberMe) {
                store.saveStudentCredentials(creds)
                store.saveStudentCabinetData(data)
            }
        }

    /** Обновляет данные по запомненным учётным данным; null, если входа ещё не было. */
    open suspend fun refresh(): Result<StudentCabinetData>? {
        val creds = credentials ?: return null
        return api.loginAndFetch(creds.login, creds.password).onSuccess { data ->
            if (creds.rememberMe) store.saveStudentCabinetData(data)
        }
    }

    open suspend fun logout() {
        credentials = null
        store.clearStudentSession()
    }
}
