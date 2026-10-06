package by.iposdev.watchso.data

import android.util.Log
import mitsoschedule.core.network.ServerConfig
import mitsoschedule.core.network.StudentWebWorker
import mitsoschedule.core.network.WebWorker
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.Inet4Address
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

private const val TAG = "WatchNetwork"

/**
 * Сетевые особенности часов: предпочитаем IPv4 и терпим расхождение системных часов
 * при проверке TLS-сертификата (на часах время часто сбито).
 */
private fun OkHttpClient.Builder.applyWatchQuirks() {
    dns { hostname ->
        val addresses = Dns.SYSTEM.lookup(hostname)
        val ipv4 = addresses.filterIsInstance<Inet4Address>()
        if (ipv4.isNotEmpty()) ipv4 else addresses
    }

    val builder = this
    try {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as KeyStore?)
        val defaultTrustManager = factory.trustManagers.firstOrNull { it is X509TrustManager } as? X509TrustManager
        if (defaultTrustManager != null) {
            val resilientTrustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    defaultTrustManager.checkClientTrusted(chain, authType)
                }

                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    try {
                        defaultTrustManager.checkServerTrusted(chain, authType)
                    } catch (e: Exception) {
                        if (isTimeSkewException(e) && !chain.isNullOrEmpty()) {
                            try {
                                val cert = chain[0]
                                val validDate = Date(cert.notBefore.time + 60_000L)
                                for (c in chain) {
                                    c.checkValidity(validDate)
                                }
                                return
                            } catch (e2: Exception) {
                                throw e
                            }
                        }
                        throw e
                    }
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> = defaultTrustManager.acceptedIssuers

                private fun isTimeSkewException(e: Throwable?): Boolean {
                    var current = e
                    while (current != null) {
                        if (current is CertificateNotYetValidException || current is CertificateExpiredException) return true
                        if (current.message?.contains("timestamp check failed", ignoreCase = true) == true) return true
                        current = current.cause
                    }
                    return false
                }
            }

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(resilientTrustManager), SecureRandom())
            builder.sslSocketFactory(sslContext.socketFactory, resilientTrustManager)
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to configure resilient SSL: ${e.message}")
    }
}

fun createWatchWebWorker(): WebWorker = WebWorker(
    client = ServerConfig.newHttpClient { applyWatchQuirks() }
)

fun createWatchStudentWebWorker(): StudentWebWorker = StudentWebWorker(
    client = ServerConfig.newHttpClient(connectTimeoutSec = 20) { applyWatchQuirks() }
)
