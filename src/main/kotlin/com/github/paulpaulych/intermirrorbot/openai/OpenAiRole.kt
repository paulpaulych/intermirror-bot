package com.github.paulpaulych.intermirrorbot.openai

import com.fasterxml.jackson.annotation.JsonProperty

enum class OpenAiRole {
    @JsonProperty("system")
    SYSTEM,
    @JsonProperty("user")
    USER,
    @JsonProperty("assistant")
    ASSISTANT
}