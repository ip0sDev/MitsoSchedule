package mitsoschedule.app

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mitsoschedule.app.data.DaySchedule
import mitsoschedule.app.data.Lesson
import mitsoschedule.app.data.OptionItem
import mitsoschedule.app.data.PreferencesManager
import mitsoschedule.app.data.ServerHealth
import mitsoschedule.app.data.SubgroupInfo
import mitsoschedule.app.data.UserSelection
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class WebWorker(
    private var baseUrl: String = PreferencesManager.DEFAULT_SERVER_URL
) {

    private val TAG = "WebWorker"
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun updateBaseUrl(newUrl: String) {
        baseUrl = newUrl.trimEnd('/')
    }

    suspend fun checkHealth(): ServerHealth {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/health")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext ServerHealth(status = "error", service = "university", version = "unknown")
                    }
                    val body = response.body?.string().orEmpty()
                    json.decodeFromString<ServerHealth>(body)
                }
            } catch (e: Exception) {
                Log.e(TAG, "checkHealth failed: ${e.message}", e)
                ServerHealth(status = "unreachable", service = "university", version = "")
            }
        }
    }

    suspend fun getCSRFTokenAndFaculties(): Pair<String, List<OptionItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val list = fetchList("$baseUrl/api/v1/schedule/faculties")
                Pair("", list)
            } catch (e: Exception) {
                Log.e(TAG, "getCSRFTokenAndFaculties failed: ${e.message}", e)
                Pair("", emptyList())
            }
        }
    }

    suspend fun fetchEducationForms(facultyId: String): List<OptionItem> {
        val encodedFaculty = URLEncoder.encode(facultyId, "UTF-8")
        return fetchList("$baseUrl/api/v1/schedule/forms?facultyId=$encodedFaculty")
    }

    suspend fun fetchCourses(facultyId: String, formId: String): List<OptionItem> {
        val encodedFaculty = URLEncoder.encode(facultyId, "UTF-8")
        val encodedForm = URLEncoder.encode(formId, "UTF-8")
        return fetchList("$baseUrl/api/v1/schedule/courses?facultyId=$encodedFaculty&formId=$encodedForm")
    }

    suspend fun fetchGroups(facultyId: String, formId: String, courseId: String): List<OptionItem> {
        val encodedFaculty = URLEncoder.encode(facultyId, "UTF-8")
        val encodedForm = URLEncoder.encode(formId, "UTF-8")
        val encodedCourse = URLEncoder.encode(courseId, "UTF-8")
        return fetchList("$baseUrl/api/v1/schedule/groups?facultyId=$encodedFaculty&formId=$encodedForm&courseId=$encodedCourse")
    }

    suspend fun fetchWeeks(facultyId: String, formId: String, courseId: String, groupId: String): List<OptionItem> {
        val encodedFaculty = URLEncoder.encode(facultyId, "UTF-8")
        val encodedForm = URLEncoder.encode(formId, "UTF-8")
        val encodedCourse = URLEncoder.encode(courseId, "UTF-8")
        val encodedGroup = URLEncoder.encode(groupId, "UTF-8")
        return fetchList("$baseUrl/api/v1/schedule/weeks?facultyId=$encodedFaculty&formId=$encodedForm&courseId=$encodedCourse&groupId=$encodedGroup")
    }

    data class ScheduleFetchResult(
        val weeks: List<OptionItem> = emptyList(),
        val daySchedules: List<DaySchedule> = emptyList()
    )

    suspend fun fetchScheduleAndAvailableWeeks(selection: UserSelection): ScheduleFetchResult {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Fetch weeks list
                val weeks = fetchWeeks(
                    selection.facultyId,
                    selection.formId.ifBlank { "Dnevnaya" },
                    selection.courseId,
                    selection.groupId
                )

                // 2. Fetch all weeks schedule in one request
                val days = fetchScheduleInternal(selection, weekId = "all")
                ScheduleFetchResult(weeks = weeks, daySchedules = days)
            } catch (e: Exception) {
                Log.e(TAG, "fetchScheduleAndAvailableWeeks failed: ${e.message}", e)
                ScheduleFetchResult()
            }
        }
    }

    suspend fun fetchSchedule(selection: UserSelection): List<DaySchedule> {
        return withContext(Dispatchers.IO) {
            try {
                fetchScheduleInternal(selection, weekId = selection.weekId.ifBlank { "1" })
            } catch (e: Exception) {
                Log.e(TAG, "fetchSchedule failed: ${e.message}", e)
                emptyList()
            }
        }
    }

    private suspend fun fetchScheduleInternal(selection: UserSelection, weekId: String): List<DaySchedule> {
        return withContext(Dispatchers.IO) {
            val urlBuilder = "$baseUrl/api/v1/schedule".toHttpUrlOrNull()?.newBuilder()
                ?: return@withContext emptyList()

            urlBuilder.addQueryParameter("facultyId", selection.facultyId)
            urlBuilder.addQueryParameter("formId", selection.formId.ifBlank { "Dnevnaya" })
            urlBuilder.addQueryParameter("courseId", selection.courseId)
            urlBuilder.addQueryParameter("groupId", selection.groupId)
            urlBuilder.addQueryParameter("weekId", weekId)

            val request = Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "fetchSchedule request failed: code ${response.code}")
                    return@withContext emptyList()
                }
                val body = response.body?.string().orEmpty()
                json.decodeFromString<List<DaySchedule>>(body)
            }
        }
    }

    private suspend fun fetchList(url: String): List<OptionItem> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "fetchList failed for $url: ${response.code}")
                        return@withContext emptyList()
                    }
                    val body = response.body?.string().orEmpty()
                    json.decodeFromString<List<OptionItem>>(body)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchList for $url: ${e.message}", e)
                emptyList()
            }
        }
    }

    // Helper functions preserved for backward compatibility and tests
    fun parseScheduleString(scheduleText: String): List<DaySchedule> {
        val daySchedules = mutableListOf<DaySchedule>()
        if (scheduleText.isBlank()) return emptyList()

        val dayHeaderRegex = """(Понедельник|Вторник|Среда|Четверг|Пятница|Суббота|Воскресенье),\s*(\d{1,2}\s+\p{L}+)""".toRegex()
        val dayMatches = dayHeaderRegex.findAll(scheduleText).toList()

        if (dayMatches.isEmpty()) return emptyList()

        dayMatches.forEachIndexed { index, matchResult ->
            val dayTitle = matchResult.groupValues[1]
            val dateSubtitle = matchResult.groupValues[2]

            val dayBlockStartIndex = matchResult.range.last + 1
            val dayBlockEndIndex = if (index < dayMatches.size - 1) {
                dayMatches[index + 1].range.first
            } else {
                scheduleText.length
            }

            var dayContent = scheduleText.substring(dayBlockStartIndex, dayBlockEndIndex).trim()
            val lessonsBlockMarker = "Время Дисциплина и преподаватель Аудитория"
            if (dayContent.startsWith(lessonsBlockMarker)) {
                dayContent = dayContent.substring(lessonsBlockMarker.length).trim()
            }

            val lessons = mutableListOf<Lesson>()
            if (dayContent.isNotBlank()) {
                val lessonParts = dayContent.split(Regex("""(?=\b\d{1,2}[.:]\d{2}\s*[-–—]\s*\d{1,2}[.:]\d{2})"""))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                lessonParts.forEach { part ->
                    val parsed = parseSingleLesson(part)
                    lessons.add(parsed)
                }
            }

            val cleanedLessons = cleanDayLessons(lessons)
            val mergedLessons = groupLessonsByTime(cleanedLessons)
            if (mergedLessons.isNotEmpty()) {
                daySchedules.add(DaySchedule(dayTitle = "$dayTitle, $dateSubtitle", dateSubtitle = dateSubtitle, lessons = mergedLessons))
            }
        }

        return daySchedules
    }

    private fun parseSingleLesson(raw: String): Lesson {
        val timeRegex = Regex("""^(\d{1,2}[.:]\d{2}\s*[-–—]\s*\d{1,2}[.:]\d{2})""")
        var timeMatch = timeRegex.find(raw)
        if (timeMatch == null) {
            timeMatch = Regex("""\b(\d{1,2}[.:]\d{2}\s*[-–—]\s*\d{1,2}[.:]\d{2})\b""").find(raw)
        }

        val text = if (timeMatch != null) {
            raw.removeRange(timeMatch.range).trim()
        } else {
            raw.trim()
        }

        val rawTime = timeMatch?.value
        val formattedTime = rawTime?.let { formatTimeRange(it) }

        if (text.contains("(нет занятий)", ignoreCase = true)) {
            return Lesson(
                time = formattedTime ?: rawTime,
                subject = "Нет занятий",
                rawText = raw,
                isEmptyWindow = true
            )
        }

        if (text.contains(" / ") || text.contains(" // ")) {
            val parts = text.split(Regex("""\s*/\s*|\s*//\s*"""))
            val hasMultipleSubgroups = parts.size > 1 && parts.any { p ->
                LEADING_SUBGROUP_REGEX.containsMatchIn(p) || INLINE_SUBGROUP_REGEXES.any { it.containsMatchIn(p) }
            }

            if (hasMultipleSubgroups) {
                val extractedSubgroups = parts.mapIndexed { index, part ->
                    val (sgName, afterSg) = extractSubgroupFromText(part)
                    val (room, afterRoom) = extractRoomFromText(afterSg)
                    val (teacher, afterTeacher) = extractTeacherFromText(afterRoom)
                    val (type, _) = extractTypeFromText(afterTeacher)
                    SubgroupInfo(
                        subgroup = sgName ?: "${index + 1} подгруппа",
                        teacher = teacher,
                        room = room,
                        subject = null,
                        type = type
                    )
                }.sortedBy { it.subgroup }

                val (commonType, _) = extractTypeFromText(text)
                return Lesson(
                    time = formattedTime ?: rawTime,
                    subject = cleanSubjectTitle(text),
                    type = commonType,
                    rawText = raw,
                    isEmptyWindow = false,
                    subgroups = extractedSubgroups
                )
            }
        }

        val (subgroup, textAfterSg) = extractSubgroupFromText(text)
        val (type, textAfterType) = extractTypeFromText(textAfterSg)
        val (teacher, textAfterTeacher) = extractTeacherFromText(textAfterType)
        val (room, textAfterRoom) = extractRoomFromText(textAfterTeacher)
        val cleanedSubject = cleanSubjectTitle(textAfterRoom)

        return Lesson(
            time = formattedTime ?: rawTime,
            subject = if (cleanedSubject.isNotBlank()) cleanedSubject else raw,
            teacher = teacher,
            room = room,
            type = type,
            rawText = raw,
            isEmptyWindow = false,
            subgroup = subgroup
        )
    }

    companion object {
        val TYPE_REGEX = Regex(
            """\((лек|практ|сем|лаб|консультация|зачет|экзамен|практ/сем|сем/практ)\)""",
            RegexOption.IGNORE_CASE
        )

        val TEACHER_REGEX = Regex(
            """\b(?:(?:[Пп]рофессор|[Дд]оцент|[Пп]реподаватель|[Сс]т\.\s*преподаватель)\s+)?([А-ЯЁ][а-яё]+)\s+([А-ЯЁ]\.\s*[А-ЯЁ]\.?|[А-ЯЁ]\.)"""
        )

        val LEADING_SUBGROUP_REGEX = Regex("""^([1-4])\.\s*""")

        val INLINE_SUBGROUP_REGEXES = listOf(
            Regex("""(?:\(?\b([1-4])\s*(?:[-–—]\s*(?:я|ая))?\s*(?:п/?г|п\.г\.|подгр(?:уппа|\.)?|группа)\b\)?)""", RegexOption.IGNORE_CASE),
            Regex("""(?:\(?\b(?:п/?г|п\.г\.|подгр(?:уппа|\.)?|группа)\s*([1-4])\b\)?)""", RegexOption.IGNORE_CASE)
        )

        val EXPLICIT_ROOM_REGEX = Regex(
            """(?i)\b(?:ауд\.|каб\.|аудитория|кабинет|зал|с/з|спортзал|актовый\s*зал)\s*([0-9а-яА-Яa-zA-Z/-]+(?:\s*-\s*[0-9а-яА-Яa-zA-Z/-]+)?(?:\s*\([а-яА-Яa-zA-Z0-9.\s]+\))?)"""
        )

        val PAREN_ROOM_REGEX = Regex(
            """\b(\d{1,3}(?:\s*-\s*\d{1,3})?\s*\([а-яА-Яa-zA-Z0-9.\s]+\))"""
        )

        val RANGE_ROOM_REGEX = Regex(
            """(?<=\s|^)(\d{1,3}\s*-\s*\d{1,3})(?=\s|$|[.,;/-])"""
        )

        val SPECIAL_ROOM_WORDS_REGEX = Regex(
            """(?i)\b(спортзал|с/з|актовый\s*зал)\b"""
        )

        val STANDALONE_NUMBER_ROOM_REGEX = Regex(
            """(?<=\s|^)(\d{2,3})(?=\s*$|\s*[А-ЯЁ][а-яё]+|\s*[(/])"""
        )

        fun extractSubgroupFromText(text: String): Pair<String?, String> {
            val current = text.trim()
            val leadingMatch = LEADING_SUBGROUP_REGEX.find(current)
            if (leadingMatch != null) {
                val num = leadingMatch.groupValues[1]
                return Pair("$num подгруппа", current.removeRange(leadingMatch.range).trim())
            }

            for (regex in INLINE_SUBGROUP_REGEXES) {
                val match = regex.find(current)
                if (match != null) {
                    val groupNum = match.groupValues.getOrNull(1)?.ifBlank { null }
                        ?: match.groupValues.getOrNull(2)?.ifBlank { null }
                    val subgroup = if (groupNum != null) "$groupNum подгруппа" else match.value.trim().removeSurrounding("(", ")")
                    val cleaned = (current.substring(0, match.range.first) + " " + current.substring(match.range.last + 1)).trim()
                    return Pair(subgroup, cleaned)
                }
            }

            return Pair(null, current)
        }

        fun extractRoomFromText(text: String): Pair<String?, String> {
            val current = text.trim()

            val explicitMatch = EXPLICIT_ROOM_REGEX.find(current)
            if (explicitMatch != null) {
                val rawVal = explicitMatch.value.trim()
                val cleaned = (current.substring(0, explicitMatch.range.first) + " " + current.substring(explicitMatch.range.last + 1)).trim()
                return Pair(rawVal, cleaned)
            }

            val specialMatch = SPECIAL_ROOM_WORDS_REGEX.find(current)
            if (specialMatch != null) {
                val rawVal = specialMatch.value.trim()
                val cleaned = (current.substring(0, specialMatch.range.first) + " " + current.substring(specialMatch.range.last + 1)).trim()
                return Pair(rawVal, cleaned)
            }

            val parenMatch = PAREN_ROOM_REGEX.find(current)
            if (parenMatch != null) {
                val rawVal = parenMatch.value.trim()
                val formatted = "ауд. $rawVal"
                val cleaned = (current.substring(0, parenMatch.range.first) + " " + current.substring(parenMatch.range.last + 1)).trim()
                return Pair(formatted, cleaned)
            }

            val rangeMatch = RANGE_ROOM_REGEX.find(current)
            if (rangeMatch != null) {
                val rawVal = rangeMatch.value.trim()
                val formatted = "ауд. $rawVal"
                val cleaned = (current.substring(0, rangeMatch.range.first) + " " + current.substring(rangeMatch.range.last + 1)).trim()
                return Pair(formatted, cleaned)
            }

            val numMatch = STANDALONE_NUMBER_ROOM_REGEX.find(current)
            if (numMatch != null) {
                val rawVal = numMatch.value.trim()
                val formatted = "ауд. $rawVal"
                val cleaned = (current.substring(0, numMatch.range.first) + " " + current.substring(numMatch.range.last + 1)).trim()
                return Pair(formatted, cleaned)
            }

            return Pair(null, current)
        }

        fun extractTypeFromText(text: String): Pair<String?, String> {
            val match = TYPE_REGEX.find(text) ?: return Pair(null, text)
            val rawType = match.groupValues[1].lowercase()
            val formatted = when {
                rawType.contains("лек") -> "Лекция"
                rawType.contains("практ") || rawType.contains("сем") -> "Практика / Семинар"
                rawType.contains("лаб") -> "Лабораторная"
                rawType.contains("зачет") -> "Зачет"
                rawType.contains("экзамен") -> "Экзамен"
                else -> match.value
            }
            val cleaned = (text.substring(0, match.range.first) + " " + text.substring(match.range.last + 1)).trim()
            return Pair(formatted, cleaned)
        }

        fun extractTeacherFromText(text: String): Pair<String?, String> {
            val match = TEACHER_REGEX.find(text) ?: return Pair(null, text)
            val teacher = match.value.trim()
            val cleaned = (text.substring(0, match.range.first) + " " + text.substring(match.range.last + 1)).trim()
            return Pair(teacher, cleaned)
        }

        fun cleanSubjectTitle(subject: String): String {
            if (subject.isBlank()) return ""
            val parts = subject.split(Regex("""\s*/\s*|\s*//\s*"""))
            val cleanedParts = parts.map { part ->
                var s = part.trim()
                s = s.replace(LEADING_SUBGROUP_REGEX, "")
                for (regex in INLINE_SUBGROUP_REGEXES) {
                    s = s.replace(regex, "")
                }
                s = s.replace(EXPLICIT_ROOM_REGEX, "")
                s = s.replace(PAREN_ROOM_REGEX, "")
                s = s.replace(RANGE_ROOM_REGEX, "")
                s = s.replace(SPECIAL_ROOM_WORDS_REGEX, "")
                s = s.replace(TYPE_REGEX, "")
                s = s.replace(TEACHER_REGEX, "")
                s = s.replace(Regex("""^[\s.,;/-]+|[\s.,;/-]+$"""), "")
                s.replace(Regex("""\s+"""), " ").trim()
            }.filter { it.isNotBlank() }

            val distinct = cleanedParts.distinctBy { it.lowercase() }
            return when {
                distinct.isEmpty() -> subject.replace(LEADING_SUBGROUP_REGEX, "").trim()
                distinct.size == 1 -> distinct.first()
                else -> distinct.joinToString(" / ")
            }
        }

        fun normalizeLesson(lesson: Lesson): Lesson {
            if (lesson.isEmptyWindow) return lesson
            val cleanedSubject = cleanSubjectTitle(lesson.subject)

            if (lesson.subgroups.isNotEmpty()) {
                val fullContext = "${lesson.rawText} ${lesson.subject}"
                val normalizedSubgroups = lesson.subgroups.map { sg ->
                    var room = sg.room
                    val teacher = sg.teacher

                    if (room.isNullOrBlank()) {
                        val sgNum = Regex("""\b([1-4])\b""").find(sg.subgroup)?.groupValues?.get(1)
                        if (sgNum != null) {
                            val chunkMatch = Regex("""(?:^|\s|/)$sgNum[.\s][^/]*""").find(fullContext)
                            if (chunkMatch != null) {
                                val (extractedRoom, _) = extractRoomFromText(chunkMatch.value)
                                if (extractedRoom != null) {
                                    room = extractedRoom
                                }
                            }
                        }
                    }

                    if (room.isNullOrBlank()) {
                        val (extractedRoom, _) = extractRoomFromText(fullContext)
                        room = extractedRoom
                    }

                    sg.copy(
                        room = room,
                        teacher = teacher,
                        subject = if (sg.subject != null) cleanSubjectTitle(sg.subject) else null
                    )
                }.sortedBy { it.subgroup }

                return lesson.copy(
                    subject = cleanedSubject,
                    room = null,
                    teacher = null,
                    subgroups = normalizedSubgroups
                )
            }

            val fullRaw = if (lesson.rawText.isNotBlank()) lesson.rawText else lesson.subject
            val rawWithoutTime = fullRaw.replace(Regex("""^(\d{1,2}[.:]\d{2}\s*[-–—]\s*\d{1,2}[.:]\d{2})\s*"""), "").trim()

            val hasSubgroup1 = Regex("""(?<=\s|^)1\.\s+|(?<=\s|^)1\s*п/?г""", RegexOption.IGNORE_CASE).containsMatchIn(rawWithoutTime)
            val hasSubgroup2 = Regex("""(?<=\s|^)2\.\s+|(?<=\s|^)2\s*п/?г""", RegexOption.IGNORE_CASE).containsMatchIn(rawWithoutTime)
            val hasMultipleSubgroups = (hasSubgroup1 && hasSubgroup2) ||
                    (lesson.subject.contains(" / ") && (lesson.subject.contains("63") || lesson.subject.contains("62") || lesson.subject.contains("ауд")))

            if (hasMultipleSubgroups) {
                val parts = if (rawWithoutTime.contains(" / ")) {
                    rawWithoutTime.split(Regex("""\s*/\s*"""))
                } else if (rawWithoutTime.contains("\n")) {
                    rawWithoutTime.split("\n")
                } else {
                    rawWithoutTime.split(Regex("""(?<=\s|^)(?=[1-4]\.\s+)""")).filter { it.isNotBlank() }
                }

                if (parts.size > 1) {
                    val extractedSubgroups = parts.mapIndexed { index, part ->
                        val (sgName, afterSg) = extractSubgroupFromText(part)
                        val (room, afterRoom) = extractRoomFromText(afterSg)
                        val (teacher, _) = extractTeacherFromText(afterRoom)
                        val (type, _) = extractTypeFromText(part)
                        SubgroupInfo(
                            subgroup = sgName ?: "${index + 1} подгруппа",
                            teacher = teacher,
                            room = room,
                            subject = null,
                            type = type ?: lesson.type
                        )
                    }.sortedBy { it.subgroup }

                    return lesson.copy(
                        subject = cleanedSubject,
                        room = null,
                        teacher = null,
                        subgroups = extractedSubgroups
                    )
                }
            }

            var singleRoom = lesson.room
            if (singleRoom.isNullOrBlank()) {
                val (foundRoom, _) = extractRoomFromText("${lesson.subject} ${lesson.rawText}")
                singleRoom = foundRoom
            }

            var singleSubgroup = lesson.subgroup
            if (singleSubgroup == null) {
                val (foundSubgroup, _) = extractSubgroupFromText("${lesson.subject} ${lesson.rawText}")
                singleSubgroup = foundSubgroup
            }

            return lesson.copy(
                subject = cleanedSubject,
                room = singleRoom,
                subgroup = singleSubgroup
            )
        }

        fun groupLessonsByTime(lessons: List<Lesson>): List<Lesson> {
            if (lessons.isEmpty()) return emptyList()

            val normalizedList = lessons.map { normalizeLesson(it) }
            val result = mutableListOf<Lesson>()
            var i = 0
            while (i < normalizedList.size) {
                val current = normalizedList[i]
                if (current.isEmptyWindow || current.time.isNullOrBlank()) {
                    result.add(current)
                    i++
                    continue
                }

                val sameTimeLessons = mutableListOf(current)
                var j = i + 1
                while (j < normalizedList.size && !normalizedList[j].isEmptyWindow && normalizedList[j].time == current.time) {
                    sameTimeLessons.add(normalizedList[j])
                    j++
                }

                if (sameTimeLessons.size == 1) {
                    if (current.subgroup != null && current.subgroups.isEmpty()) {
                        result.add(
                            current.copy(
                                subgroups = listOf(
                                    SubgroupInfo(
                                        subgroup = current.subgroup,
                                        teacher = current.teacher,
                                        room = current.room,
                                        subject = null,
                                        type = current.type
                                    )
                                )
                            )
                        )
                    } else {
                        result.add(current)
                    }
                    i++
                } else {
                    val subgroupList = sameTimeLessons.flatMap { item ->
                        if (item.subgroups.isNotEmpty()) {
                            item.subgroups
                        } else {
                            listOf(
                                SubgroupInfo(
                                    subgroup = item.subgroup ?: "Подгруппа",
                                    teacher = item.teacher,
                                    room = item.room,
                                    subject = null,
                                    type = item.type
                                )
                            )
                        }
                    }.sortedBy { it.subgroup }

                    val commonSubject = cleanSubjectTitle(sameTimeLessons.joinToString(" / ") { it.subject })
                    val commonType = sameTimeLessons.firstOrNull { !it.type.isNullOrBlank() }?.type

                    val mergedLesson = Lesson(
                        time = current.time,
                        subject = commonSubject,
                        type = commonType,
                        teacher = null,
                        room = null,
                        rawText = sameTimeLessons.joinToString("\n") { it.rawText },
                        isEmptyWindow = false,
                        subgroup = null,
                        subgroups = subgroupList
                    )
                    result.add(mergedLesson)
                    i = j
                }
            }
            return result
        }

        fun cleanDayLessons(lessons: List<Lesson>): List<Lesson> {
            val firstRealIndex = lessons.indexOfFirst { !it.isEmptyWindow }
            val lastRealIndex = lessons.indexOfLast { !it.isEmptyWindow }
            if (firstRealIndex == -1 || lastRealIndex == -1) return emptyList()
            return lessons.subList(firstRealIndex, lastRealIndex + 1)
        }

        fun formatTimeRange(rawTime: String): String {
            val match = Regex("""(\d{1,2})[.:](\d{2})\s*[-–—]\s*(\d{1,2})[.:](\d{2})""").find(rawTime)
            if (match != null) {
                val (h1, m1, h2, m2) = match.destructured
                return "${h1.padStart(2, '0')}.${m1} — ${h2.padStart(2, '0')}.${m2}"
            }
            return rawTime
        }
    }
}