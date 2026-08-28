package by.iposdev.mitsotest.data

import kotlinx.serialization.Serializable

@Serializable
data class OptionItem(
    val id: String,
    val name: String
)

@Serializable
data class DepDropResponse(
    val output: List<OptionItem> = emptyList(),
    val selected: String = ""
)

@Serializable
enum class LessonType(val title: String) {
    LECTURE("Лекция"),
    PRACTICE("Практика / Семинар"),
    LAB("Лабораторная"),
    EXAM("Экзамен / Зачет"),
    CONSULTATION("Консультация"),
    OTHER("Занятие")
}

@Serializable
data class Lesson(
    val time: String? = null,
    val subject: String = "",
    val teacher: String? = null,
    val room: String? = null,
    val type: String? = null,
    val rawText: String = "",
    val isEmptyWindow: Boolean = false
)

@Serializable
data class DaySchedule(
    val dayTitle: String,
    val dateSubtitle: String? = null,
    val lessons: List<Lesson> = emptyList()
)

@Serializable
data class UserSelection(
    val facultyId: String = "",
    val facultyName: String = "",
    val formId: String = "Dnevnaya",
    val formName: String = "Дневная",
    val courseId: String = "",
    val courseName: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val weekId: String = "0",
    val weekName: String = "Текущая неделя"
) {
    val isComplete: Boolean
        get() = facultyId.isNotBlank() && courseId.isNotBlank() && groupId.isNotBlank()

    val displaySummary: String
        get() = if (groupName.isNotBlank()) {
            "$groupName • $courseName"
        } else if (facultyName.isNotBlank()) {
            facultyName
        } else {
            "Выберите группу"
        }
}
