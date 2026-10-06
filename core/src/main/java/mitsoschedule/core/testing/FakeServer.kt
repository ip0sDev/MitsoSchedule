package mitsoschedule.core.testing

import mitsoschedule.core.network.ServerConfig
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Подставной сервер для тестов: отвечает JSON по пути запроса, остальные пути дают 404.
 * Ответы можно менять по ходу теста ([respond], [goOffline]).
 */
class FakeServer(initial: Map<String, String> = emptyMap()) {
    private val routes = HashMap(initial)

    @Volatile
    private var offline = false

    /** Пути (с query) всех полученных запросов. */
    val requests = CopyOnWriteArrayList<String>()

    fun respond(path: String, body: String) {
        synchronized(routes) { routes[path] = body }
    }

    fun goOffline() {
        offline = true
    }

    fun goOnline() {
        offline = false
    }

    fun client(): OkHttpClient = ServerConfig.newHttpClient {
        addInterceptor(
            Interceptor { chain ->
                val url = chain.request().url
                requests += url.encodedPath + (url.query?.let { "?$it" } ?: "")
                if (offline) throw IOException("нет сети")
                val body = synchronized(routes) { routes[url.encodedPath] }
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(if (body != null) 200 else 404)
                    .message("fake")
                    .body((body ?: "").toResponseBody("application/json".toMediaType()))
                    .build()
            }
        )
    }
}
