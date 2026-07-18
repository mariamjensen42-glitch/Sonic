package com.cycling.data.api

import com.cycling.data.api.dto.DeepSeekRequest
import com.cycling.data.api.dto.DeepSeekResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepSeekApiServiceImpl @Inject constructor() : DeepSeekApiService {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val BASE_URL = "https://api.deepseek.com/v1/chat/completions"
        private const val CONTENT_TYPE = "application/json; charset=utf-8"
    }

    override suspend fun chat(
        apiKey: String,
        request: DeepSeekRequest
    ): Result<DeepSeekResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val requestBody = json.encodeToString(request)
                .toRequestBody(CONTENT_TYPE.toMediaType())

            val httpRequest = Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", CONTENT_TYPE)
                .post(requestBody)
                .build()

            val response = client.newCall(httpRequest).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw Exception("API Error: ${response.code} - $errorBody")
            }

            val responseBody = response.body?.string()
                ?: throw Exception("Empty response body")

            json.decodeFromString<DeepSeekResponse>(responseBody)
        }
    }
}
