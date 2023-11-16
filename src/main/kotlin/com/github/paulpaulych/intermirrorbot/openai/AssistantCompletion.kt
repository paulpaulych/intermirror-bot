package com.github.paulpaulych.intermirrorbot.openai

data class AssistantCompletion(
    val created: Int,
    val message: OpenAiMessage
)