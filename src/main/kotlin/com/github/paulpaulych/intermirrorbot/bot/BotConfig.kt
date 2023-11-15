package com.github.paulpaulych.intermirrorbot.bot

import com.github.paulpaulych.intermirrorbot.service.ChannelService
import kotlinx.coroutines.runBlocking
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
    fun init(channelService: ChannelService, updateHandlers: Set<TypedUpdateHandler<*>>) = TgBot(
        token = token,
        onUpdate = { update, bot ->
            HandlersCollection(handlers = updateHandlers).onUpdate(update, bot)
        }
    )
}

@Component
class BotInitializer(
    private val bot: TgBot
) {

    @EventListener(ApplicationReadyEvent::class)
    fun doSomethingAfterStartup() {
        runBlocking {
            bot.start()
        }
    }
}