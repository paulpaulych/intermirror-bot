package com.github.paulpaulych.intermirrorbot.openai

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

class OpenAiClient(
    builder: WebClient.Builder,
    baseUrl: String,
    apiKey: String
) {
    companion object {
        private const val MODEL = "gpt-3.5-turbo"
        private const val TEMPERATURE = 0
    }

    private val webClient: WebClient = builder
        .baseUrl(baseUrl)
        .defaultHeaders { headers -> headers.setBearerAuth(apiKey) }
        .build()

    suspend fun getAssistantResponse(chatHistory: List<OpenAiMessage>): AssistantCompletion {
        val request = OpenAiRequest(MODEL, chatHistory, TEMPERATURE)
        return webClient.post()
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OpenAiResponse::class.java)
            .flatMap { openAiResponse -> toAssistantCompletion(openAiResponse) }
            .onErrorMap { e -> toReadableError(e) }
            .switchIfEmpty(Mono.error(IllegalStateException("OpenAI request resulted with empty mono")))
            .awaitSingle()
    }

    private fun toReadableError(e: Throwable): Throwable {
        return if (e is WebClientResponseException) {
            IllegalStateException("OpenAI request failed: status=" + e.statusCode + " body=" + e.responseBodyAsString)
        } else IllegalStateException("OpenAI request failed", e)
    }

    private fun toAssistantCompletion(openAiResponse: OpenAiResponse): Mono<AssistantCompletion> {
        return if (openAiResponse.choices.isEmpty()) {
            Mono.error(IllegalStateException("choices is empty"))
        } else Mono.just(AssistantCompletion(openAiResponse.created, openAiResponse.choices[0].message))
    }
}