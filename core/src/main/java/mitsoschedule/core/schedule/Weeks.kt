package mitsoschedule.core.schedule

import mitsoschedule.core.model.DaySchedule
import mitsoschedule.core.model.OptionItem
import mitsoschedule.core.model.UserSelection
import java.time.LocalDate

/** Работа со списком недель: виртуальный пункт «все недели», соседние недели, выбор текущей. */
object Weeks {
    const val ALL_ID = "ALL"
    val ALL_OPTION = OptionItem(id = ALL_ID, name = "Все доступные недели")

    /** Добавляет «Все доступные недели» в начало, если недель две и больше. */
    fun withAllOption(weeks: List<OptionItem>): List<OptionItem> {
        if (weeks.size >= 2 && weeks.none { it.id == ALL_ID }) {
            return listOf(ALL_OPTION) + weeks
        }
        return weeks
    }

    /** Реальные недели без виртуального пункта: по ним ходят стрелки навигации. */
    fun real(weeks: List<OptionItem>): List<OptionItem> = weeks.filter { it.id != ALL_ID }

    fun hasPrevious(weeks: List<OptionItem>, currentId: String): Boolean {
        val list = real(weeks)
        return list.size > 1 && list.indexOfFirst { it.id == currentId } > 0
    }

    fun hasNext(weeks: List<OptionItem>, currentId: String): Boolean {
        val list = real(weeks)
        if (list.size <= 1) return false
        val index = list.indexOfFirst { it.id == currentId }
        return index >= 0 && index < list.size - 1
    }

    fun previous(weeks: List<OptionItem>, currentId: String): OptionItem? =
        if (hasPrevious(weeks, currentId)) real(weeks)[real(weeks).indexOfFirst { it.id == currentId } - 1] else null

    fun next(weeks: List<OptionItem>, currentId: String): OptionItem? =
        if (hasNext(weeks, currentId)) real(weeks)[real(weeks).indexOfFirst { it.id == currentId } + 1] else null

    /** Недели, восстановленные из кэшированного расписания (чтобы навигация работала офлайн). */
    fun fromSchedule(days: List<DaySchedule>): List<OptionItem> = days
        .map { OptionItem(it.weekId, it.weekName) }
        .filter { it.id.isNotBlank() && it.id != "0" }
        .distinctBy { it.id }

    /** Id недели-заглушки (пустой или "0"), который нужно заменить на реальный. */
    fun isUnresolved(weekId: String): Boolean = weekId.isBlank() || weekId == "0"

    /**
     * Если сохранённая неделя не определена (пустая, "0") или, при [requireKnown],
     * отсутствует в расписании, возвращает выбор с текущей неделей; иначе null.
     */
    fun resolveStored(
        saved: UserSelection,
        weeks: List<OptionItem>,
        days: List<DaySchedule>,
        requireKnown: Boolean,
        today: LocalDate = LocalDate.now()
    ): UserSelection? {
        if (saved.weekId == ALL_ID) return null
        val known = days.map { it.weekId }.filter { it.isNotBlank() }.toSet()
        val invalid = isUnresolved(saved.weekId) || (requireKnown && saved.weekId !in known)
        if (!invalid) return null

        val currentId = ScheduleDates.findCurrentWeekId(weeks, days, today) ?: return null
        val name = weeks.find { it.id == currentId }?.name
            ?: days.firstOrNull { it.weekId == currentId }?.weekName
            ?: saved.weekName
        return saved.copy(weekId = currentId, weekName = name)
    }

    /**
     * Какую неделю выбрать после загрузки расписания. [weeks] это список с сервера (без «все недели»).
     * Возвращает null, если выбор менять не нужно.
     */
    fun resolveAfterRefresh(
        current: UserSelection,
        weeks: List<OptionItem>,
        days: List<DaySchedule>,
        isManualRefresh: Boolean,
        today: LocalDate = LocalDate.now()
    ): OptionItem? {
        if (current.weekId == ALL_ID || weeks.isEmpty()) return null

        val currentWeekId = ScheduleDates.findCurrentWeekId(weeks, days, today)
        val currentWeekHasLessons = days.any { it.weekId == current.weekId }
        val shouldReset = isManualRefresh ||
                isUnresolved(current.weekId) ||
                weeks.none { it.id == current.weekId } ||
                (!currentWeekHasLessons && currentWeekId != null)

        val targetId = if (shouldReset) {
            currentWeekId ?: weeks.firstOrNull { it.id != ALL_ID }?.id ?: weeks.firstOrNull()?.id
        } else {
            current.weekId
        }

        val resolved = weeks.find { it.id == targetId } ?: return null
        return if (resolved.id != current.weekId || resolved.name != current.weekName) resolved else null
    }
}
