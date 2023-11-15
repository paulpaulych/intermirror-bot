package com.github.paulpaulych.intermirrorbot.bot

import dev.inmo.tgbotapi.bot.TelegramBot
import org.slf4j.LoggerFactory
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.startGettingOfUpdatesByLongPolling
import dev.inmo.tgbotapi.types.ALL_UPDATES_LIST
import dev.inmo.tgbotapi.types.update.abstracts.Update
import dev.inmo.tgbotapi.updateshandlers.UpdatesFilter
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*

class TgBot(
    token: String,
    private val onUpdate: suspend (Update, TelegramBot) -> Unit
): Closeable {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private lateinit var job: Job
    private val theBot = telegramBot(token)

    suspend fun start() {
        val me = theBot.getMe();
        logger.info("Bot started: ${me.username}")
        logger.info(me.toString())

        this.job = theBot.startGettingOfUpdatesByLongPolling(object : UpdatesFilter {
            override val allowedUpdates = ALL_UPDATES_LIST
            override val asUpdateReceiver: suspend (Update) -> Unit = { update: Update ->
                logger.info("update: {}", update)
                try {
                    onUpdate(update, theBot)
                } catch (e: Exception) {
                    logger.error("error handling update id=${update.updateId}", e)
                }
            }
        })
    }

    override fun close() {
        job.cancel()
    }
}
