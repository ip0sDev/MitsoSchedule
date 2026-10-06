package mitsoschedule.core.network

/** Почему запрос к серверу не удался: UI показывает разные сообщения для разных причин. */
data class ApiError(
    val kind: Kind,
    val message: String? = null,
    val httpCode: Int? = null
) {
    enum class Kind {
        /** Нет сети, сервер недоступен, таймаут. */
        NETWORK,

        /** Сервер ответил кодом, отличным от 2xx. */
        HTTP,

        /** Ответ получен, но разобрать его не удалось. */
        PARSE
    }
}

/** Результат запроса: значение или причина неудачи (в отличие от «пустого списка», который неоднозначен). */
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>

    val errorOrNull: ApiError? get() = (this as? Failure)?.error
}

fun <T> ApiResult<List<T>>.orEmpty(): List<T> = (this as? ApiResult.Success)?.value ?: emptyList()

fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.value
