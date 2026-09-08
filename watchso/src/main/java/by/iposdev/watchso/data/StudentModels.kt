package by.iposdev.watchso.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class StudentCabinetData(
    val fullName: String = "",
    val accountDate: String = "",
    val balance: String = "0.00",
    val mainDebt: String = "0.00",
    val penalty: String = "0.00",
    val moodleGroup: String = "",
    val moodleLogin: String = "",
    val moodlePassword: String = "",
    val lastFetchedTime: String = ""
) {
    val isDebt: Boolean
        get() {
            val debtNum = mainDebt.replace(",", ".").replace(" ", "").toDoubleOrNull() ?: 0.0
            val penaltyNum = penalty.replace(",", ".").replace(" ", "").toDoubleOrNull() ?: 0.0
            val balanceNum = balance.replace(",", ".").replace(" ", "").toDoubleOrNull() ?: 0.0
            return debtNum > 0.0 || penaltyNum > 0.0 || balanceNum < 0.0
        }

    val initialsName: String
        get() {
            val parts = fullName.split(" ").filter { it.isNotBlank() }
            return if (parts.size >= 3) {
                "${parts[0]} ${parts[1].firstOrNull() ?: ""}. ${parts[2].firstOrNull() ?: ""}."
            } else if (parts.isNotEmpty()) {
                parts[0]
            } else {
                "Студент"
            }
        }
}

@Immutable
@Serializable
data class StudentAuthCredentials(
    val login: String = "",
    val password: String = "",
    val rememberMe: Boolean = true
)
