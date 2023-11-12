package com.github.paulpaulych.intermirrorbot.bot

import com.github.paulpaulych.intermirrorbot.bot.commands.IdCommandHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component


@Configuration
class BotConfig(
    @Value("\${telegram.bot.token}")
    private val token: String
) {

    @Bean
    fun init() = TgBot(
        token = token,
        commandHandlers = mapOf(
            "id" to IdCommandHandler
        )
    )
}

@Component
class BotInitializer(
    private val bot: TgBot
) {

    @EventListener(ApplicationReadyEvent::class)
    fun doSomethingAfterStartup() {
        bot.start()
    }
}