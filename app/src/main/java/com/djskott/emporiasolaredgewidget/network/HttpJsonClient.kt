package com.djskott.emporiasolaredgewidget.network

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class HttpJsonClient {
    fun getObject(url: String, headers: Map<String, String> = emptyMap()): JSONObject {
        return request(url = url, method = "GET", headers = headers) as JSONObject
    }

    fun getArray(url: String, headers: Map<String, String> = emptyMap()): JSONArray {
        return request(url = url, method = "GET", headers = headers) as JSONArray
    }

    fun postObject(
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: JSONObject,
    ): JSONObject {
        return request(
            url = url,
            method = "POST",
            headers = headers,
            body = body.toString(),
        ) as JSONObject
    }

    private fun request(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String? = null,
    ): Any {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doInput = true
            if (body != null) {
                doOutput = true
            }
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
        }

        if (body != null) {
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(body)
            }
        }

        val responseText = readText(connection)
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException(
                "HTTP ${connection.responseCode} ${connection.responseMessage}: $responseText",
            )
        }

        val trimmed = responseText.trim()
        if (trimmed.startsWith("{")) {
            return JSONObject(trimmed)
        }
        if (trimmed.startsWith("[")) {
            return JSONArray(trimmed)
        }
        throw IllegalStateException("Unexpected JSON response: $trimmed")
    }

    private fun readText(connection: HttpURLConnection): String {
        val stream = connection.errorStream ?: connection.inputStream
        BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { reader ->
            return reader.readText()
        }
    }

    companion object {
        private const val TIMEOUT_MS = 15_000
    }
}

