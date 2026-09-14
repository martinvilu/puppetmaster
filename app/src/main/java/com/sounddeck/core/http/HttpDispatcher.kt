package com.sounddeck.core.http

import android.os.SystemClock
import android.util.Log
import com.sounddeck.core.model.DynamicPadState
import com.sounddeck.core.model.HttpAction
import com.sounddeck.core.model.HttpDefaults
import com.sounddeck.core.model.LastHttpTransaction
import com.sounddeck.core.model.ResponseMapping
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class HttpDispatcher(
    private val httpDefaults: HttpDefaults = HttpDefaults()
) {
    private val tag = "HttpDispatcher"

    private val json = Json { ignoreUnknownKeys = true }

    private val baseClient = OkHttpClient.Builder()
        .connectTimeout(httpDefaults.defaultTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(httpDefaults.defaultTimeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(httpDefaults.defaultTimeoutMs, TimeUnit.MILLISECONDS)
        .build()

    private val _lastTransaction = MutableStateFlow<LastHttpTransaction?>(null)
    val lastTransaction: StateFlow<LastHttpTransaction?> = _lastTransaction.asStateFlow()

    private val _transactionHistory = MutableStateFlow<List<LastHttpTransaction>>(emptyList())
    val transactionHistory: StateFlow<List<LastHttpTransaction>> = _transactionHistory.asStateFlow()

    suspend fun execute(
        action: HttpAction,
        defaults: HttpDefaults = httpDefaults,
        contextVariables: Map<String, String> = emptyMap()
    ): DynamicPadState = withContext(Dispatchers.IO) {
        val startTime = SystemClock.elapsedRealtime()
        val timeoutMs = action.timeoutMs ?: defaults.defaultTimeoutMs

        // Resolve dynamic variables in URL and body (e.g. {timestamp}, {random})
        val resolvedUrl = resolveVariables(action.url, contextVariables)
        val resolvedBody = action.body?.let { resolveVariables(it, contextVariables) }

        val client = if (timeoutMs != defaults.defaultTimeoutMs) {
            baseClient.newBuilder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build()
        } else {
            baseClient
        }

        // Merge headers: defaults + action headers
        val allHeaders = defaults.baseHeaders.toMutableMap().apply {
            putAll(action.headers)
        }

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = when (action.method.uppercase()) {
            "POST", "PUT", "PATCH" -> (resolvedBody ?: "").toRequestBody(mediaType)
            else -> null
        }

        val request = try {
            val builder = Request.Builder()
                .url(resolvedUrl)
                .headers(allHeaders.toHeaders())

            when (action.method.uppercase()) {
                "GET" -> builder.get()
                "POST" -> builder.post(requestBody ?: "".toRequestBody(mediaType))
                "PUT" -> builder.put(requestBody ?: "".toRequestBody(mediaType))
                "PATCH" -> builder.patch(requestBody ?: "".toRequestBody(mediaType))
                "DELETE" -> builder.delete(requestBody)
                else -> builder.get()
            }
            builder.build()
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            val errorTx = LastHttpTransaction(
                method = action.method,
                url = resolvedUrl,
                statusCode = 0,
                durationMs = duration,
                message = "Invalid Request: ${e.localizedMessage ?: "Unknown error"}",
                requestHeaders = allHeaders,
                requestBody = resolvedBody
            )
            recordTransaction(errorTx)
            return@withContext DynamicPadState()
        }

        try {
            client.newCall(request).execute().use { response ->
                val duration = SystemClock.elapsedRealtime() - startTime
                val responseBodyString = response.body?.string() ?: ""
                val snippet = if (responseBodyString.length > 60) {
                    responseBodyString.take(60) + "..."
                } else {
                    responseBodyString.replace("\n", " ").trim()
                }

                val tx = LastHttpTransaction(
                    method = action.method,
                    url = resolvedUrl,
                    statusCode = response.code,
                    durationMs = duration,
                    message = if (response.isSuccessful) snippet.ifEmpty { "Success" } else "${response.code} ${response.message}",
                    requestHeaders = allHeaders,
                    requestBody = resolvedBody
                )
                recordTransaction(tx)

                if (response.isSuccessful && responseBodyString.isNotBlank()) {
                    return@withContext parseResponse(responseBodyString, action.responseMapping)
                }

                return@withContext DynamicPadState()
            }
        } catch (e: SocketTimeoutException) {
            val duration = SystemClock.elapsedRealtime() - startTime
            val tx = LastHttpTransaction(
                method = action.method,
                url = resolvedUrl,
                statusCode = 408,
                durationMs = duration,
                message = "Timeout (${timeoutMs}ms)",
                requestHeaders = allHeaders,
                requestBody = resolvedBody
            )
            recordTransaction(tx)
            DynamicPadState()
        } catch (e: IOException) {
            val duration = SystemClock.elapsedRealtime() - startTime
            val tx = LastHttpTransaction(
                method = action.method,
                url = resolvedUrl,
                statusCode = 0,
                durationMs = duration,
                message = "Network Error: ${e.localizedMessage ?: "Failed"}",
                requestHeaders = allHeaders,
                requestBody = resolvedBody
            )
            recordTransaction(tx)
            DynamicPadState()
        } catch (e: Exception) {
            val duration = SystemClock.elapsedRealtime() - startTime
            val tx = LastHttpTransaction(
                method = action.method,
                url = resolvedUrl,
                statusCode = 0,
                durationMs = duration,
                message = "Error: ${e.localizedMessage ?: "Unknown"}",
                requestHeaders = allHeaders,
                requestBody = resolvedBody
            )
            recordTransaction(tx)
            DynamicPadState()
        }
    }

    private fun resolveVariables(text: String, contextVariables: Map<String, String>): String {
        var resolved = text
        val now = System.currentTimeMillis()
        val defaultVars = mapOf(
            "timestamp" to now.toString(),
            "iso_time" to java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date(now)),
            "random" to (1000..9999).random().toString()
        )

        (defaultVars + contextVariables).forEach { (key, value) ->
            resolved = resolved.replace("{{$key}}", value).replace("{$key}", value)
        }
        return resolved
    }

    suspend fun testRequest(
        action: HttpAction,
        defaults: HttpDefaults = httpDefaults,
        contextVariables: Map<String, String> = emptyMap()
    ): LastHttpTransaction = withContext(Dispatchers.IO) {
        execute(action, defaults, contextVariables)
        _lastTransaction.value ?: LastHttpTransaction(
            method = action.method,
            url = action.url,
            statusCode = 0,
            durationMs = 0,
            message = "No response"
        )
    }

    private fun recordTransaction(tx: LastHttpTransaction) {
        _lastTransaction.value = tx
        val currentHistory = _transactionHistory.value.toMutableList()
        currentHistory.add(0, tx)
        if (currentHistory.size > 50) {
            currentHistory.removeLast()
        }
        _transactionHistory.value = currentHistory
    }

    /**
     * Extracts dynamic pad properties from response JSON body using lightweight JSONPath expressions.
     */
    fun parseResponse(jsonBody: String, mapping: ResponseMapping?): DynamicPadState {
        if (mapping == null) return DynamicPadState()

        return try {
            val rootElement = json.parseToJsonElement(jsonBody)

            val percentage = mapping.jsonPathPercentage?.let { path ->
                extractJsonValue(rootElement, path)?.let { rawValue ->
                    rawValue.toDoubleOrNull()?.toFloat()?.coerceIn(0f, 100f)
                }
            }

            val textBadge = mapping.jsonPathTextBadge?.let { path ->
                extractJsonValue(rootElement, path)
            }

            val icon = mapping.jsonPathIcon?.let { path ->
                extractJsonValue(rootElement, path)
            }

            DynamicPadState(
                percentage = percentage,
                textBadge = textBadge,
                icon = icon
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse response with mapping: ${e.message}")
            DynamicPadState()
        }
    }

    /**
     * Evaluates a dot/bracket JSONPath like "$.data.temperature" or "status" or "items[0].value"
     */
    private fun extractJsonValue(element: JsonElement, path: String): String? {
        val cleanPath = path.trim().removePrefix("$").removePrefix(".")
        if (cleanPath.isEmpty()) {
            return element.jsonPrimitive.contentOrNull
        }

        val segments = parsePathSegments(cleanPath)
        var current: JsonElement = element

        for (segment in segments) {
            when {
                segment.isArrayIndex -> {
                    val array = current as? JsonArray ?: return null
                    if (segment.index in 0 until array.size) {
                        current = array[segment.index]
                    } else {
                        return null
                    }
                }
                else -> {
                    val obj = current as? JsonObject ?: return null
                    current = obj[segment.key] ?: return null
                }
            }
        }

        return try {
            current.jsonPrimitive.contentOrNull ?: current.toString()
        } catch (e: Exception) {
            current.toString()
        }
    }

    private data class PathSegment(
        val isArrayIndex: Boolean,
        val key: String = "",
        val index: Int = 0
    )

    private fun parsePathSegments(path: String): List<PathSegment> {
        val result = mutableListOf<PathSegment>()
        val parts = path.split(".")

        for (part in parts) {
            if (part.contains("[") && part.endsWith("]")) {
                val propName = part.substringBefore("[")
                if (propName.isNotEmpty()) {
                    result.add(PathSegment(isArrayIndex = false, key = propName))
                }
                val indexStr = part.substringAfter("[").removeSuffix("]")
                val index = indexStr.toIntOrNull() ?: 0
                result.add(PathSegment(isArrayIndex = true, index = index))
            } else if (part.isNotEmpty()) {
                result.add(PathSegment(isArrayIndex = false, key = part))
            }
        }
        return result
    }
}
