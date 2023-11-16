package com.github.paulpaulych.intermirrorbot.openai

import com.fasterxml.jackson.annotation.JsonProperty


internal data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Int
)

internal data class OpenAiResponse(
    val id: String,
    val `object`: String,
    val created: Int,
    val model: String,
    val choices: List<OpenAiChoice>
) {

    data class OpenAiChoice(
        val index: Int,
        val message: OpenAiMessage,
        @field:JsonProperty("finish_reason")
        val finishReason: String
    )

    data class Usage(
        @JsonProperty("prompt_tokens")
        val promptTokens: Int,
        @JsonProperty("completion_tokens")
        val completionTokens: Int,
        @JsonProperty("total_tokens")
        val totalTokens: Int
    )
}