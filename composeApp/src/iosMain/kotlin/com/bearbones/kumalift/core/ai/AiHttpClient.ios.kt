package com.bearbones.kumalift.core.ai

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.dataUsingEncoding
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setTimeoutInterval
import platform.Foundation.setValue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual object PlatformAiHttpTransport : AiHttpTransport {
    override suspend fun get(
        url: String,
        headers: Map<String, String>,
        timeoutMs: Int
    ): AiHttpResponse = executeRequest(
        url = url,
        method = "GET",
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
        method = "POST",
        headers = headers,
        body = body,
        timeoutMs = timeoutMs
    )

    private suspend fun executeRequest(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
        timeoutMs: Int
    ): AiHttpResponse = suspendCancellableCoroutine { cont ->
        val nsUrl = NSURL.URLWithString(url)
            ?: run {
                cont.resumeWithException(IllegalArgumentException("Invalid URL"))
                return@suspendCancellableCoroutine
            }
        val request = NSMutableURLRequest.requestWithURL(nsUrl).apply {
            setHTTPMethod(method)
            setTimeoutInterval(timeoutMs / 1000.0)
            if (body != null) {
                setValue("application/json; charset=UTF-8", forHTTPHeaderField = "Content-Type")
                setHTTPBody(NSString.create(string = body).dataUsingEncoding(NSUTF8StringEncoding))
            }
            headers.forEach { (k, v) -> setValue(v, forHTTPHeaderField = k) }
        }
        val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, response, error ->
            if (!cont.isActive) return@dataTaskWithRequest
            if (error != null) {
                cont.resumeWithException(RuntimeException(error.localizedDescription))
                return@dataTaskWithRequest
            }
            val httpResponse = response as? NSHTTPURLResponse
            val code = httpResponse?.statusCode?.toInt() ?: 0
            val text = if (data != null) {
                NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString().orEmpty()
            } else {
                ""
            }
            cont.resume(AiHttpResponse(statusCode = code, body = text))
        }
        cont.invokeOnCancellation { task.cancel() }
        task.resume()
    }
}
