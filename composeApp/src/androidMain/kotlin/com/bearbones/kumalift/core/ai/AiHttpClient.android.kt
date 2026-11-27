package com.bearbones.kumalift.core.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.bearbones.kumalift.core.timer.AndroidAppProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual object PlatformAiHttpTransport : AiHttpTransport {
    private val jsonMediaType = "application/json; charset=UTF-8".toMediaType()

    private val ipv4FirstDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val all = Dns.SYSTEM.lookup(hostname)
            val ipv4 = all.filterIsInstance<Inet4Address>()
            val other = all.filter { it !is Inet4Address }
            return ipv4 + other
        }
    }

    private val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(ipv4FirstDns)
            .connectTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Suppress("DEPRECATION")
    override fun isNetworkOnline(): Boolean {
        val context = AndroidAppProvider.appContext ?: return true
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            cm.activeNetworkInfo?.isConnected == true
        }
    }

    override suspend fun get(
        url: String,
        headers: Map<String, String>,
        timeoutMs: Int
    ): AiHttpResponse = executeRequest(
        url = url,
        headers = headers,
        body = null,
        timeoutMs = timeoutMs
    )

    override suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
        timeoutMs: Int
    ): AiHttpResponse = executeRequest(
        url = url,
        headers = headers,
        body = body,
        timeoutMs = timeoutMs
    )

    private suspend fun executeRequest(
        url: String,
        headers: Map<String, String>,
        body: String?,
        timeoutMs: Int
    ): AiHttpResponse = suspendCancellableCoroutine { cont ->
        val reqBuilder = Request.Builder().url(url)
        headers.forEach { (key, value) -> reqBuilder.addHeader(key, value) }
        if (body != null) {
            reqBuilder.post(body.toRequestBody(jsonMediaType))
        } else {
            reqBuilder.get()
        }

        val client = baseClient.newBuilder()
            .readTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
            .callTimeout((timeoutMs + 25_000).toLong(), TimeUnit.MILLISECONDS)
            .build()

        val call = client.newCall(reqBuilder.build())
        cont.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use { res ->
                        val responseText = res.body?.string().orEmpty()
                        if (cont.isActive) {
                            cont.resume(AiHttpResponse(statusCode = res.code, body = responseText))
                        }
                    }
                } catch (t: Throwable) {
                    if (cont.isActive) cont.resumeWithException(t)
                }
            }
        })
    }
}
