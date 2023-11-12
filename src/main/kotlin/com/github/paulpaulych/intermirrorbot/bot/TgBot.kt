package com.github.paulpaulych.intermirrorbot.bot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.HandleContact
import com.github.kotlintelegrambot.dispatcher.handlers.HandleNewChatMembers
import com.github.kotlintelegrambot.dispatcher.handlers.Handler
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.Update
import org.slf4j.LoggerFactory
import com.github.paulpaulych.intermirrorbot.bot.commands.CommandHandler

class TgBot(
    private val token: String,
    private val commandHandlers: Map<String, CommandHandler>
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val theBot = bot {
        this.token = this@TgBot.token
        dispatch {
            for ((cmd, handler) in commandHandlers) {
                addHandler(object : Handler {
                    override fun checkUpdate(update: Update) = true

                    override suspend fun handleUpdate(bot: Bot, update: Update) {
                        logger.info("encountered update: {}", update)
                    }
                })
//                command(cmd) {
//                    logger.debug("received start command: {}, params: {}", message, args.toList())
//                    val sender = sender(bot)
//                    try {
//                        handler.process(message, args, sender)
//                    } catch (e: Throwable) {
//                        logger.error("error during handling command from ${message.chat.id}", e)
//                        sender.send(ChatId.fromId(message.chat.id), "Unexpected error occurred: ${e.message}")
//                    }
//                }
            }
        }
    }

    fun start() {
//        theBot.startPolling()
//        val me = theBot.getMe().get()
//        logger.info("Bot started: ${me.username}")
//        logger.info(me.toString())
        val updates = theBot.getUpdates()
        theBot.sendMessage()
        logger.info("updates: {}", updates)
    }

    private fun sender(bot: Bot): Sender = object : Sender {
        override fun send(chatId: ChatId, msg: String) {
            bot.sendMessage(chatId, msg).get()
        }

        override fun reply(to: Message, msg: String) {
            bot.sendMessage(ChatId.fromId(to.chat.id), msg, replyToMessageId = to.messageId).get()
        }
    }
}
