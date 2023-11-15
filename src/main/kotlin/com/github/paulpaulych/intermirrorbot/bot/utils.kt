package com.github.paulpaulych.intermirrorbot.bot

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.types.update.abstracts.Update

class HandlersCollection(
    private val handlers: Set<TypedUpdateHandler<*>>
) {

    suspend fun onUpdate(update: Update, bot: TelegramBot) {
        handlers.forEach { handler ->
            tryToApplyHandler(update, bot, handler)
        }
    }

    private suspend fun <T : Any> tryToApplyHandler(update: Update, bot: TelegramBot, handler: TypedUpdateHandler<T>) {
        val typedUpdate = handler.checkType(update)
        if (typedUpdate != null) {
            handler.handle(typedUpdate, bot)
        }
    }
}

interface TypedUpdateHandler<T : Any> {
    fun checkType(update: Update): T?
    suspend fun handle(update: T, bot: TelegramBot)
}

