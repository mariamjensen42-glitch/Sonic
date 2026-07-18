package com.cycling.data.api

import com.cycling.data.api.dto.DeepSeekRequest
import com.cycling.data.api.dto.DeepSeekResponse

interface DeepSeekApiService {
    suspend fun chat(
        apiKey: String,
        request: DeepSeekRequest
    ): Result<DeepSeekResponse>
}
