package com.github.paulpaulych.intermirrorbot.bot.commands

import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.paulpaulych.intermirrorbot.bot.Sender
import org.slf4j.LoggerFactory

object IdCommandHandler: CommandHandler {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun process(message: Message, params: List<String>, sender: Sender) {
        logger.debug("received id command from chatId=${message.chat.id}: {}", message)
        sender.send(ChatId.fromId(message.chat.id), "Chat ID: ${message.chat.id}")
    }
}
