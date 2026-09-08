package by.iposdev.watchso.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
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

@Immutable
@Serializable
data class DaySchedule(
    val dayTitle: String,
    val dateSubtitle: String = "",
    val lessons: List<Lesson> = emptyList()
)

@Immutable
@Serializable
data class OptionItem(
    val id: String,
    val name: String
)

@Immutable
@Serializable
data class DepDropResponse(
    val output: List<DepDropItem> = emptyList(),
    val selected: String = ""
)

@Immutable
@Serializable
data class DepDropItem(
    val id: String,
    val name: String
)

@Immutable
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
    val weekId: String = "",
    val weekName: String = ""
) {
    val isComplete: Boolean
        get() = facultyId.isNotBlank() && courseId.isNotBlank() && groupId.isNotBlank()

    val shortLabel: String
        get() = if (groupName.isNotBlank()) {
            if (courseName.isNotBlank()) "$groupName • $courseName" else groupName
        } else {
            "Выбрать группу"
        }
}
