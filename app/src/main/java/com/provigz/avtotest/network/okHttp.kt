package com.provigz.avtotest.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException

private val gson = Gson()

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

suspend fun <T> newRequest(
    url: String,
    method: String,
    request: Any,
    responseClass: Class<T>
): T {
    return withContext(Dispatchers.IO) {
        val requestBody = gson.toJson(request).toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url(url)
            .method(method, requestBody)
            .build()

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

        gson.fromJson(responseBody, responseClass)
    }
}