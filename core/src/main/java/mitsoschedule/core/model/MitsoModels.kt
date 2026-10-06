package mitsoschedule.core.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class OptionItem(
    val id: String,
    val name: String
)

@Immutable
@Serializable
data class SubgroupInfo(
    val subgroup: String = "",
    val teacher: String? = null,
    val room: String? = null,
    val subject: String? = null,
    val type: String? = null
)

@Immutable
@Serializable
data class Lesson(
    val time: String? = null,
    val subject: String = "",
    val teacher: String? = null,
    val room: String? = null,
    val type: String? = null,
    val rawText: String = "",
    val isEmptyWindow: Boolean = false,
    val subgroup: String? = null,
    val subgroups: List<SubgroupInfo> = emptyList()
)

@Immutable
@Serializable
data class DaySchedule(
    val date: String? = null,
    val dayTitle: String,
    val dateSubtitle: String? = null,
    val lessons: List<Lesson> = emptyList(),
    val weekId: String = "0",
    val weekName: String = ""
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
    val weekId: String = "0",
    val weekName: String = "Текущая неделя"
) {
    val isComplete: Boolean
        get() = facultyId.isNotBlank() && courseId.isNotBlank() && groupId.isNotBlank()

    /** Выбор сохранён в старом формате (числовые id вместо слагов). */
    val hasValidFormat: Boolean
        get() = isComplete && !facultyId.all { it.isDigit() } && !courseId.all { it.isDigit() }

    val displaySummary: String
        get() = if (groupName.isNotBlank()) {
            "$groupName • $courseName"
        } else if (facultyName.isNotBlank()) {
            facultyName
        } else {
            "Выберите группу"
        }

    val shortLabel: String
        get() = if (groupName.isNotBlank()) {
            if (courseName.isNotBlank()) "$groupName • $courseName" else groupName
        } else {
            "Выбрать группу"
        }
}

@Immutable
@Serializable
data class ServerHealth(
    val status: String = "unknown",
    val service: String = "",
    val version: String = ""
) {
    val isHealthy: Boolean
        get() = status.equals("healthy", ignoreCase = true) || status.equals("ok", ignoreCase = true)
}
