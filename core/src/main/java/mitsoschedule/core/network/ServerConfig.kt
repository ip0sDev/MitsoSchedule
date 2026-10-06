package mitsoschedule.core.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ServerConfig {
    const val DEFAULT_SERVER_URL = "https://university.visorlink.org"

    /**
     * Базовый клиент для обращений к серверу. [configure] позволяет платформе
     * добавить свои особенности (например, DNS и TLS для часов).
     */
    fun newHttpClient(
        connectTimeoutSec: Long = 15,
        readTimeoutSec: Long = 25,
        writeTimeoutSec: Long = connectTimeoutSec,
        configure: OkHttpClient.Builder.() -> Unit = {}
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeoutSec, TimeUnit.SECONDS)
        .readTimeout(readTimeoutSec, TimeUnit.SECONDS)
        .writeTimeout(writeTimeoutSec, TimeUnit.SECONDS)
        .apply(configure)
        .build()
}
