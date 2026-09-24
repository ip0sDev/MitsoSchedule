package mitsoschedule.app.data

import kotlinx.serialization.Serializable

@Serializable
data class StudentCabinetData(
    val fullName: String = "",
    val accountDate: String = "",
    val balance: String = "0.00",
    val mainDebt: String = "0.00",
    val penalty: String = "0.00",
    val isDebt: Boolean = false,
    val moodleGroup: String = "",
    val moodleLogin: String = "",
    val moodlePassword: String = "",
    val lastFetchedTime: String = ""
)

@Serializable
data class StudentAuthCredentials(
    val login: String = "",
    val password: String = "",
    val rememberMe: Boolean = true
)
