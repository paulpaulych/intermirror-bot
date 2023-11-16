package com.github.paulpaulych.intermirrorbot.openai

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class OpenAiConfiguration {

    @Bean
    fun openAiClient(
        builder: WebClient.Builder,
        @Value("\${openai.apiKey}") apiKey: String,
        @Value("\${openai.apiUrl}") apiUrl: String
    ): OpenAiClient {
        return OpenAiClient(builder, apiUrl, apiKey)
    }
}