package mitsoschedule.core.schedule

import mitsoschedule.core.model.DaySchedule
import mitsoschedule.core.model.OptionItem
import mitsoschedule.core.model.ServerHealth
import mitsoschedule.core.model.UserSelection
import mitsoschedule.core.network.ApiError
import mitsoschedule.core.network.ApiResult
import mitsoschedule.core.network.WebWorker
import mitsoschedule.core.network.orEmpty
import mitsoschedule.core.storage.ScheduleStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Загрузка и кэширование расписания: общая логика телефона и часов.
 * ViewModel держит только состояние экрана и вызывает методы отсюда.
 *
 * @param updateTimePattern формат подписи «обновлено» (у часов короче, чем у телефона).
 */
open class ScheduleRepository(
    private val api: WebWorker,
    private val store: ScheduleStore,
    private val updateTimePattern: String = "dd.MM.yyyy HH:mm"
) {

    /**
     * Варианты следующих шагов выбора для уже сохранённой группы.
     * [error] заполнен, если хотя бы один запрос не удался (загруженное остаётся в списках).
     */
    data class SelectionOptions(
        val forms: List<OptionItem> = emptyList(),
        val courses: List<OptionItem> = emptyList(),
        val groups: List<OptionItem> = emptyList(),
        val weeks: List<OptionItem> = emptyList(),
        val error: ApiError? = null
    )

    /**
     * Результат обновления.
     * @property weeks недели с сервера вместе с пунктом «все недели»; пусто, если сервер их не вернул.
     * @property days расписание; пусто, если данных нет (кэш при этом не перезаписывается).
     * @property selection выбор с актуальной неделей (он уже сохранён, если изменился).
     * @property updateTime время сохранения кэша, null если ничего не сохранялось.
     * @property error причина, по которой данных нет; null, если сервер просто ответил пустым расписанием.
     */
    data class RefreshResult(
        val weeks: List<OptionItem>,
        val days: List<DaySchedule>,
        val selection: UserSelection,
        val updateTime: String?,
        val error: ApiError? = null
    )

    // Шаги каскада выбора и служебные запросы: ViewModel не обращается к WebWorker напрямую.
    open suspend fun faculties(): ApiResult<List<OptionItem>> = api.fetchFaculties()

    open suspend fun forms(facultyId: String): ApiResult<List<OptionItem>> = api.fetchEducationForms(facultyId)

    open suspend fun courses(facultyId: String, formId: String): ApiResult<List<OptionItem>> =
        api.fetchCourses(facultyId, formId)

    open suspend fun groups(facultyId: String, formId: String, courseId: String): ApiResult<List<OptionItem>> =
        api.fetchGroups(facultyId, formId, courseId)

    open suspend fun weeks(
        facultyId: String,
        formId: String,
        courseId: String,
        groupId: String
    ): ApiResult<List<OptionItem>> = api.fetchWeeks(facultyId, formId, courseId, groupId)

    open suspend fun checkHealth(): ServerHealth = api.checkHealth()

    open fun setServerUrl(url: String) = api.updateBaseUrl(url)

    /** Загружает формы, курсы, группы и недели, нужные для отображения текущего выбора. */
    open suspend fun loadOptions(selection: UserSelection): SelectionOptions {
        if (selection.facultyId.isBlank()) return SelectionOptions()

        val forms = api.fetchEducationForms(selection.facultyId)
        val formId = selection.formId.ifBlank { forms.orEmpty().firstOrNull()?.id ?: DEFAULT_FORM_ID }
        val courses = api.fetchCourses(selection.facultyId, formId)
        var options = SelectionOptions(forms.orEmpty(), courses.orEmpty(), error = forms.errorOrNull ?: courses.errorOrNull)
        if (selection.courseId.isBlank()) return options

        val groups = api.fetchGroups(selection.facultyId, formId, selection.courseId)
        options = options.copy(groups = groups.orEmpty(), error = options.error ?: groups.errorOrNull)
        if (selection.groupId.isBlank()) return options

        val weeks = api.fetchWeeks(selection.facultyId, formId, selection.courseId, selection.groupId)
        return options.copy(weeks = Weeks.withAllOption(weeks.orEmpty()), error = options.error ?: weeks.errorOrNull)
    }

    /** Перезагружает расписание на все недели, подбирает неделю и сохраняет кэш. */
    open suspend fun refresh(selection: UserSelection, isManualRefresh: Boolean): RefreshResult {
        val fetched = api.fetchScheduleAndAvailableWeeks(selection)

        var updated = selection
        val weeks = if (fetched.weeks.isNotEmpty()) Weeks.withAllOption(fetched.weeks) else emptyList()
        Weeks.resolveAfterRefresh(selection, fetched.weeks, fetched.daySchedules, isManualRefresh)?.let { week ->
            updated = selection.copy(weekId = week.id, weekName = week.name)
            store.saveSelection(updated)
        }

        val updateTime = if (fetched.daySchedules.isNotEmpty()) {
            nowLabel().also { store.saveSchedule(fetched.daySchedules, it) }
        } else {
            null
        }
        return RefreshResult(weeks, fetched.daySchedules, updated, updateTime, fetched.error)
    }

    /**
     * Догружает одну неделю, которой нет в [existing]. В случае успеха возвращает объединённое
     * расписание и время сохранения (null, если сервер вернул пустую неделю); при сбое ошибку.
     */
    open suspend fun loadWeek(
        selection: UserSelection,
        week: OptionItem,
        existing: List<DaySchedule>
    ): ApiResult<Pair<List<DaySchedule>, String>?> {
        val fetched = when (val result = api.fetchSingleWeekSchedule(selection, week.id, week.name)) {
            is ApiResult.Failure -> return result
            is ApiResult.Success -> result.value
        }
        if (fetched.isEmpty()) return ApiResult.Success(null)

        val merged = existing.filter { it.weekId != week.id } + fetched
        val time = nowLabel()
        store.saveSchedule(merged, time)
        return ApiResult.Success(merged to time)
    }

    private fun nowLabel(): String = SimpleDateFormat(updateTimePattern, Locale.getDefault()).format(Date())

    private companion object {
        const val DEFAULT_FORM_ID = "Dnevnaya"
    }
}
