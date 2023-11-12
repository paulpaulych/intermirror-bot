package com.github.paulpaulych.intermirrorbot.bot.commands

import com.github.kotlintelegrambot.entities.Message
import com.github.paulpaulych.intermirrorbot.bot.Sender

fun interface CommandHandler {

    suspend fun process(
        message: Message,
        params: List<String>,
        sender: Sender
    )
}