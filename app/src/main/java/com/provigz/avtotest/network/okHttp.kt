package com.provigz.avtotest.network

import androidx.compose.runtime.MutableState
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException

val gson = Gson()

data class HTTPErrorResponseBody(
    @SerializedName("error") val error: String?,
    @SerializedName("message") val message: String?
)

private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}
private val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .build()

suspend fun newRequest(
    url: String,
    method: String,
    request: Any?
): String {
    return withContext(Dispatchers.IO) {
        val req: Request
        if (request != null) {
            val requestBody = gson.toJson(request).toRequestBody("application/json".toMediaType())
            req = Request.Builder()
                .url(url)
                .method(method, requestBody)
                .build()
        } else {
            req = Request.Builder()
                .url(url)
                .build()
        }

        val response = okHttpClient.newCall(req).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful) {
            if (responseBody != null) {
                val errorBody = gson.fromJson(
                    responseBody,
                    HTTPErrorResponseBody::class.java
                )
                throw IOException("HTTP error: ${response.code} - ${response.message} (\"${errorBody.error}: ${errorBody.message}\")")
            }

            throw IOException("HTTP error: ${response.code} - ${response.message}")
        }

        responseBody ?: throw IOException("Empty response body")
        responseBody
    }
}

suspend inline fun <reified T> newRequest(
    url: String,
    method: String,
    request: Any?,
    response: MutableState<T?>,
    finished: MutableState<Boolean>,
    error: MutableState<String?>
) {
    try {
        val responseLocal = gson.fromJson(newRequest(url, method, request), T::class.java)
        response.value = responseLocal
        finished.value = true
    } catch (e: Exception) {
        e.printStackTrace()
        error.value = e.message
    }
}

suspend inline fun <reified T> newRequestArray(
    url: String,
    method: String,
    request: Any?,
    response: MutableState<List<T>?>,
    finished: MutableState<Boolean>,
    error: MutableState<String?>
) {
    try {
        val responseLocal = gson.fromJson<List<T>>(newRequest(url, method, request), object : TypeToken<List<T>>() {}.type)
        response.value = responseLocal
        finished.value = true
    } catch (e: Exception) {
        e.printStackTrace()
        error.value = e.message
    }
}