package com.github.paulpaulych.intermirrorbot.bot

import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message


interface Sender {
    fun send(chatId: ChatId, msg: String)
    fun reply(to: Message, msg: String)
}
