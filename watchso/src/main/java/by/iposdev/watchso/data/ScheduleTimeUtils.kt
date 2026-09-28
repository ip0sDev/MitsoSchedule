package by.iposdev.watchso.data

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

enum class TodayScheduleState {
    NO_LESSONS,
    NOT_STARTED,
    ONGOING_LESSON,
    BREAK_BETWEEN_LESSONS,
    FINISHED
}

data class TodayTimeInfo(
    val state: TodayScheduleState,
    val currentLesson: Lesson? = null,
    val nextLesson: Lesson? = null,
    val currentLessonIndex: Int = -1,
    val nextLessonIndex: Int = -1,
    val infoMessage: String = "",
    val detailMessage: String = "",
    val minutesToNext: Long = 0L
)

object ScheduleTimeUtils {

    fun parseTimeRange(timeStr: String?): Pair<LocalTime, LocalTime>? {
        if (timeStr.isNullOrBlank()) return null
        val match = Regex("""(\d{1,2})[.:](\d{2})\s*[-–—]\s*(\d{1,2})[.:](\d{2})""").find(timeStr) ?: return null
        val (h1, m1, h2, m2) = match.destructured
        return try {
            val start = LocalTime.of(h1.toInt(), m1.toInt())
            val end = LocalTime.of(h2.toInt(), m2.toInt())
            Pair(start, end)
        } catch (e: Exception) {
            null
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
        val subtitle = day.dateSubtitle.trim()
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
        val oneWeekMillis = 7 * 24 * 60 * 60 * 1000L
        val elapsed = System.currentTimeMillis() - lastFetchMillis
        if (elapsed < 0L || elapsed >= oneWeekMillis) return true
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
        val startOfWeek = today.with(java.time.DayOfWeek.MONDAY)
        val endOfWeek = today.with(java.time.DayOfWeek.SUNDAY)
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
            val diff = kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(today, parsed))
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
                java.time.DayOfWeek.MONDAY -> "понедельник"
                java.time.DayOfWeek.TUESDAY -> "вторник"
                java.time.DayOfWeek.WEDNESDAY -> "среда"
                java.time.DayOfWeek.THURSDAY -> "четверг"
                java.time.DayOfWeek.FRIDAY -> "пятница"
                java.time.DayOfWeek.SATURDAY -> "суббота"
                java.time.DayOfWeek.SUNDAY -> "воскресенье"
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

    fun calculateTodayTimeInfo(lessons: List<Lesson>, now: LocalTime = LocalTime.now()): TodayTimeInfo {
        val activeLessons = lessons.filter { !it.isEmptyWindow }
        if (activeLessons.isEmpty()) {
            return TodayTimeInfo(
                state = TodayScheduleState.NO_LESSONS,
                infoMessage = "Пар сегодня нет",
                detailMessage = "Отличного дня!"
            )
        }

        val lessonTimes = activeLessons.map { parseTimeRange(it.time) }
        val firstStartTime = lessonTimes.firstOrNull { it != null }?.first
        val lastEndTime = lessonTimes.lastOrNull { it != null }?.second

        if (firstStartTime != null && now.isBefore(firstStartTime)) {
            val formattedStart = "%02d:%02d".format(firstStartTime.hour, firstStartTime.minute)
            val minutesToStart = Duration.between(now, firstStartTime).toMinutes()
            val firstLesson = activeLessons.firstOrNull()
            return TodayTimeInfo(
                state = TodayScheduleState.NOT_STARTED,
                nextLesson = firstLesson,
                nextLessonIndex = 0,
                infoMessage = "Пары ещё не начались",
                detailMessage = if (minutesToStart > 0) "1-я пара через $minutesToStart мин (в $formattedStart)" else "1-я пара в $formattedStart",
                minutesToNext = minutesToStart
            )
        }

        if (lastEndTime != null && now.isAfter(lastEndTime)) {
            return TodayTimeInfo(
                state = TodayScheduleState.FINISHED,
                infoMessage = "Пары на сегодня всё",
                detailMessage = "Отличного отдыха!"
            )
        }

        // Check if currently inside a lesson
        for (i in activeLessons.indices) {
            val range = lessonTimes[i] ?: continue
            if (!now.isBefore(range.first) && !now.isAfter(range.second)) {
                val formattedEnd = "%02d:%02d".format(range.second.hour, range.second.minute)
                val minutesLeft = Duration.between(now, range.second).toMinutes()
                val nextLesson = if (i + 1 < activeLessons.size) activeLessons[i + 1] else null
                return TodayTimeInfo(
                    state = TodayScheduleState.ONGOING_LESSON,
                    currentLesson = activeLessons[i],
                    nextLesson = nextLesson,
                    currentLessonIndex = i,
                    nextLessonIndex = if (nextLesson != null) i + 1 else -1,
                    infoMessage = "Идёт ${i + 1}-я пара",
                    detailMessage = "До $formattedEnd (${minutesLeft} мин)",
                    minutesToNext = minutesLeft
                )
            }
        }

        // Check if between lessons (break / перемена)
        for (i in 0 until activeLessons.size - 1) {
            val currentRange = lessonTimes[i]
            val nextRange = lessonTimes[i + 1]
            if (currentRange != null && nextRange != null) {
                if (now.isAfter(currentRange.second) && now.isBefore(nextRange.first)) {
                    val formattedNextStart = "%02d:%02d".format(nextRange.first.hour, nextRange.first.minute)
                    val minutesLeft = Duration.between(now, nextRange.first).toMinutes()
                    return TodayTimeInfo(
                        state = TodayScheduleState.BREAK_BETWEEN_LESSONS,
                        currentLesson = activeLessons[i],
                        nextLesson = activeLessons[i + 1],
                        currentLessonIndex = i,
                        nextLessonIndex = i + 1,
                        infoMessage = "Перемена",
                        detailMessage = if (minutesLeft > 0) "След. пара через $minutesLeft мин (в $formattedNextStart)" else "След. пара в $formattedNextStart",
                        minutesToNext = minutesLeft
                    )
                }
            }
        }

        return TodayTimeInfo(
            state = TodayScheduleState.NOT_STARTED,
            nextLesson = activeLessons.firstOrNull(),
            infoMessage = "Расписание",
            detailMessage = "Занятия запланированы"
        )
    }
}
