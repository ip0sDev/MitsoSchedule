package mitsoschedule.core.schedule

import mitsoschedule.core.model.DaySchedule
import mitsoschedule.core.model.OptionItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Даты и недели расписания: разбор «21 сентября», определение текущей недели, актуальность кэша. */
object ScheduleDates {

    const val ONE_WEEK_MILLIS = 7 * 24 * 60 * 60 * 1000L

    /** Пора ли обновить кэш: старше недели всегда, с пятницы — раз в сутки. */
    fun shouldAutoRefresh(lastFetchMillis: Long): Boolean {
        if (lastFetchMillis <= 0L) return true

        val elapsedMillis = System.currentTimeMillis() - lastFetchMillis
        if (elapsedMillis < 0) return true
        if (elapsedMillis >= ONE_WEEK_MILLIS) return true

        val dayOfWeek = LocalDate.now().dayOfWeek
        val isFridayOrLater = dayOfWeek == DayOfWeek.FRIDAY ||
                dayOfWeek == DayOfWeek.SATURDAY ||
                dayOfWeek == DayOfWeek.SUNDAY

        return if (isFridayOrLater) {
            val oneDayMillis = 24 * 60 * 60 * 1000L
            elapsedMillis >= oneDayMillis
        } else {
            false
        }
    }

    fun parseDateText(text: String, today: LocalDate = LocalDate.now()): LocalDate? {
        val trimmed = text.trim()
        val parts = trimmed.split("\\s+".toRegex())
        if (parts.size >= 2) {
            val dayNum = parts[0].toIntOrNull()
            val monthName = parts[1].lowercase()
            val monthNum = when {
                monthName.startsWith("янв") -> 1
                monthName.startsWith("фев") -> 2
                monthName.startsWith("мар") -> 3
                monthName.startsWith("апр") -> 4
                monthName.startsWith("май") || monthName.startsWith("мая") -> 5
                monthName.startsWith("июн") -> 6
                monthName.startsWith("июл") -> 7
                monthName.startsWith("авг") -> 8
                monthName.startsWith("сен") -> 9
                monthName.startsWith("окт") -> 10
                monthName.startsWith("ноя") -> 11
                monthName.startsWith("дек") -> 12
                else -> 0
            }

            if (dayNum != null && monthNum in 1..12) {
                val currentYear = today.year
                val inferredYear = if (today.monthValue == 1 && monthNum == 12) {
                    currentYear - 1
                } else if (today.monthValue == 12 && monthNum == 1) {
                    currentYear + 1
                } else {
                    currentYear
                }

                return try {
                    LocalDate.of(inferredYear, monthNum, dayNum)
                } catch (_: Exception) {
                    null
                }
            }
        }
        return null
    }

    fun parseWeekDateRange(weekName: String, today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate>? {
        val parts = weekName.split("[-–—]".toRegex())
        if (parts.size == 2) {
            val start = parseDateText(parts[0], today)
            val end = parseDateText(parts[1], today)
            if (start != null && end != null) {
                return Pair(start, end)
            }
        }
        return null
    }

    fun parseDate(day: DaySchedule, today: LocalDate = LocalDate.now()): LocalDate? {
        val subtitle = day.dateSubtitle?.trim().orEmpty()
        val fullTitle = day.dayTitle.trim()
        val dateText = if (subtitle.isNotBlank()) subtitle else fullTitle.substringAfter(",").trim()
        return parseDateText(dateText, today)
    }

    fun isScheduleOlderThanWeek(
        lastFetchMillis: Long,
        schedules: List<DaySchedule> = emptyList(),
        today: LocalDate = LocalDate.now()
    ): Boolean {
        if (lastFetchMillis <= 0L) return true
        val elapsed = System.currentTimeMillis() - lastFetchMillis
        if (elapsed < 0L || elapsed >= ONE_WEEK_MILLIS) return true
        if (schedules.isEmpty()) return true

        var hasValidDate = false
        for (day in schedules) {
            val parsed = parseDate(day, today)
            if (parsed != null) {
                hasValidDate = true
                if (!parsed.isBefore(today)) {
                    return false
                }
            }
        }
        return hasValidDate
    }

    fun findCurrentWeekId(
        weeks: List<OptionItem>,
        schedules: List<DaySchedule>,
        today: LocalDate = LocalDate.now()
    ): String? {
        // 1. Try finding a DaySchedule that matches today
        val todaySchedule = schedules.find { day ->
            val parsed = parseDate(day, today)
            parsed != null && parsed.isEqual(today)
        }
        if (todaySchedule != null && todaySchedule.weekId.isNotBlank() && todaySchedule.weekId != "0" && todaySchedule.weekId != "ALL") {
            return todaySchedule.weekId
        }

        // 2. Try finding a DaySchedule within current week (Monday..Sunday)
        val startOfWeek = today.with(DayOfWeek.MONDAY)
        val endOfWeek = today.with(DayOfWeek.SUNDAY)
        val thisWeekSchedule = schedules.find { day ->
            val parsed = parseDate(day, today)
            parsed != null && !parsed.isBefore(startOfWeek) && !parsed.isAfter(endOfWeek)
        }
        if (thisWeekSchedule != null && thisWeekSchedule.weekId.isNotBlank() && thisWeekSchedule.weekId != "0" && thisWeekSchedule.weekId != "ALL") {
            return thisWeekSchedule.weekId
        }

        // 3. Check for explicitly designated "Текущая неделя" in weeks list
        val currentNamedWeek = weeks.find { it.id != "ALL" && it.name.contains("текущ", ignoreCase = true) }
        if (currentNamedWeek != null) {
            return currentNamedWeek.id
        }

        // 4. Try parsing week date ranges from week names (e.g. "21 сентября - 27 сентября")
        for (w in weeks) {
            if (w.id == "ALL") continue
            val range = parseWeekDateRange(w.name, today)
            if (range != null && !today.isBefore(range.first) && !today.isAfter(range.second)) {
                return w.id
            }
        }

        // 5. Find the week closest to today
        var closestWeekId: String? = null
        var minDiffDays = Long.MAX_VALUE
        for (day in schedules) {
            val parsed = parseDate(day, today) ?: continue
            val diff = kotlin.math.abs(ChronoUnit.DAYS.between(today, parsed))
            if (diff < minDiffDays && day.weekId.isNotBlank() && day.weekId != "0" && day.weekId != "ALL") {
                minDiffDays = diff
                closestWeekId = day.weekId
            }
        }
        if (closestWeekId != null) return closestWeekId

        // 6. Fallback: first real week, or last week
        val realWeeks = weeks.filter { it.id != "ALL" }
        return realWeeks.firstOrNull()?.id ?: realWeeks.lastOrNull()?.id
    }

    fun findTodaySchedule(schedules: List<DaySchedule>, today: LocalDate = LocalDate.now()): DaySchedule? {
        if (schedules.isEmpty()) return null

        for (day in schedules) {
            val parsedDate = parseDate(day, today)
            if (parsedDate != null && parsedDate.isEqual(today)) {
                return day
            }
        }

        // Only fallback to day of week if NO items in schedules had valid dates
        val hasAnyParsedDate = schedules.any { parseDate(it, today) != null }
        if (!hasAnyParsedDate) {
            val dayOfWeekRu = when (today.dayOfWeek) {
                DayOfWeek.MONDAY -> "понедельник"
                DayOfWeek.TUESDAY -> "вторник"
                DayOfWeek.WEDNESDAY -> "среда"
                DayOfWeek.THURSDAY -> "четверг"
                DayOfWeek.FRIDAY -> "пятница"
                DayOfWeek.SATURDAY -> "суббота"
                DayOfWeek.SUNDAY -> "воскресенье"
            }
            return schedules.find { it.dayTitle.lowercase().startsWith(dayOfWeekRu) }
        }

        return null
    }

    fun findNextUpcomingDay(schedules: List<DaySchedule>, today: LocalDate = LocalDate.now()): DaySchedule? {
        val todaySched = findTodaySchedule(schedules, today)
        if (todaySched != null) {
            val idx = schedules.indexOf(todaySched)
            if (idx != -1 && idx < schedules.size - 1) {
                return schedules[idx + 1]
            }
        }
        return schedules.firstOrNull()
    }
}
