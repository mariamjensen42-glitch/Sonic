@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.cycling.data.api.dto

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeepSeekRequest(
    val model: String = "deepseek-chat",
    val messages: List<Message>,
    val stream: Boolean = false
) {
    @Serializable
    data class Message(
        val role: String,
        val content: String
    )
}

@Serializable
data class DeepSeekResponse(
    val id: String? = null,
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null
) {
    @Serializable
    data class Choice(
        val index: Int = 0,
        val message: Message? = null,
        @SerialName("finish_reason")
        val finishReason: String? = null
    ) {
        @Serializable
        data class Message(
            val role: String? = null,
            val content: String? = null
        )
    }

    @Serializable
    data class Usage(
        @SerialName("prompt_tokens")
        val promptTokens: Int = 0,
        @SerialName("completion_tokens")
        val completionTokens: Int = 0,
        @SerialName("total_tokens")
        val totalTokens: Int = 0
    )

    fun getContent(): String? = choices.firstOrNull()?.message?.content
}
