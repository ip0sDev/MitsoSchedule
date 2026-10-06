package mitsoschedule.core.schedule

import mitsoschedule.core.model.Lesson
import mitsoschedule.core.model.SubgroupInfo

/**
 * Очистка и нормализация пар, пришедших с сервера или из кэша:
 * выделяет аудиторию, тип, преподавателя и подгруппы из сырого текста
 * и склеивает пары с одинаковым временем в одну карточку.
 */
object ScheduleNormalizer {
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
}
