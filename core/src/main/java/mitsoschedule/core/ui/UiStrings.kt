package mitsoschedule.core.ui

import android.content.Context
import androidx.annotation.StringRes
import mitsoschedule.core.R
import mitsoschedule.core.network.ApiError

/** Доступ к строковым ресурсам из ViewModel без Context (в тестах подменяется). */
interface UiStrings {
    fun get(@StringRes id: Int, vararg args: Any): String
}

class ContextUiStrings(private val context: Context) : UiStrings {
    override fun get(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)
}

/** Короткая причина сбоя для подстановки в сообщение: «нет связи с сервером», «сервер вернул ошибку (код 500)». */
fun UiStrings.reason(error: ApiError): String = when (error.kind) {
    ApiError.Kind.NETWORK -> get(R.string.error_reason_network)
    ApiError.Kind.HTTP -> get(R.string.error_reason_http, error.httpCode ?: 0)
    ApiError.Kind.PARSE -> get(R.string.error_reason_parse)
}
