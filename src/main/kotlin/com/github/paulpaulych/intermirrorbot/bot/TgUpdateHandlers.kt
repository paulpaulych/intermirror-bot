package com.github.paulpaulych.intermirrorbot.bot

import com.github.paulpaulych.intermirrorbot.service.ChannelService
import com.github.paulpaulych.intermirrorbot.service.MirroringService
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.types.chat.ChannelChat
import dev.inmo.tgbotapi.types.chat.member.AdministratorChatMember
import dev.inmo.tgbotapi.types.chat.member.BannedChatMember
import dev.inmo.tgbotapi.types.chat.member.LeftChatMember
import dev.inmo.tgbotapi.types.update.ChannelPostUpdate
import dev.inmo.tgbotapi.types.update.MyChatMemberUpdatedUpdate
import dev.inmo.tgbotapi.types.update.abstracts.Update
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TgUpdateHandlers {

    @Bean
    fun onMyMember(channelService: ChannelService) = onUpdate<MyChatMemberUpdatedUpdate> { update ->
        val chat = update.data.chat
        if (chat !is ChannelChat) {
            logger.debug("chat is not ${ChannelChat::class.java.simpleName}. Update ignored")
            return@onUpdate
        }
        when(val newState = update.data.newChatMemberState) {
            is AdministratorChatMember -> {
                channelService.activateChannel(chat.id.chatId, chat.title)
            }
            is LeftChatMember, is BannedChatMember -> {
                channelService.deactivate(chat.id.chatId)
            }
            else -> logger.info("unexpected new member state: $newState. Update ignored")
        }
    }

    @Bean
    // TODO add on edit handle and on delete handlers
    fun onChannelPost(mirroringService: MirroringService) = onUpdate<ChannelPostUpdate> { update ->
        val message = update.data
        val text = message.text
        if (text == null) {
            logger.debug("message.text is null. Update ignored")
            return@onUpdate
        }
        if (isCommand(text)) {
            logger.info("received command: $text")
            when {
                text.startsWith("/add_src") -> {
                    mirroringService.startMirroringFromChannel(message.chat.id.chatId)
                }
                text.startsWith("/add_tgt") -> {
                    val args = parseArguments(text)
                    if (args.size < 2) {
                        throw IllegalStateException("not enough arguments for command: $text")
                    }
                    val tgtChatId = args[0].toLong()
                    val language = args[1]
                    mirroringService.addTarget(message.chat.id.chatId, tgtChatId, language)
                }
            }
            return@onUpdate
        }
        mirroringService.mirror(update.data, bot)
    }

    class HandlerContext(
        val bot: TelegramBot,
        val logger: Logger
    )

    private fun isCommand(command: String): Boolean {
        return command.startsWith("/")
    }

    private fun parseArguments(command: String): List<String> {
        return command.substringAfter('?').split("&")
    }

    private final inline fun <reified T: Update> onUpdate(crossinline handler: suspend HandlerContext.(T) -> Unit) = object : TypedUpdateHandler<T> {
        override fun checkType(update: Update): T? = update as? T
        override suspend fun handle(update: T, bot: TelegramBot) {
            val context = HandlerContext(bot, LoggerFactory.getLogger(T::class.java))
            context.handler(update)
        }
    }
}