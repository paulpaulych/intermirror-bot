package com.github.paulpaulych.intermirrorbot.bot

import com.github.paulpaulych.intermirrorbot.core.service.ChannelService
import com.github.paulpaulych.intermirrorbot.core.service.MirroringService
import com.github.paulpaulych.intermirrorbot.bot.userflows.UserFlowsManager
import com.github.paulpaulych.intermirrorbot.bot.userflows.UserAction
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.new_chat_title
import dev.inmo.tgbotapi.types.chat.ChannelChat
import dev.inmo.tgbotapi.types.chat.member.AdministratorChatMember
import dev.inmo.tgbotapi.types.chat.member.BannedChatMember
import dev.inmo.tgbotapi.types.chat.member.LeftChatMember
import dev.inmo.tgbotapi.types.message.ChatEvents.abstracts.ChatEvent
import dev.inmo.tgbotapi.types.update.CallbackQueryUpdate
import dev.inmo.tgbotapi.types.update.ChannelPostUpdate
import dev.inmo.tgbotapi.types.update.MyChatMemberUpdatedUpdate
import dev.inmo.tgbotapi.types.update.abstracts.Update
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TelegramUpdatesListeners {

    @Bean
    fun onMyMember(
        channelService: ChannelService,
        userFlowsManager: UserFlowsManager
    ) = onUpdate<MyChatMemberUpdatedUpdate> { update ->
        val chat = update.data.chat
        val chatId = chat.id.chatId
        if (chat !is ChannelChat) {
            val username = update.data.user.username?.username
                ?: throw IllegalStateException("update has no user")
            userFlowsManager.initUserInstructionsFlow(chatId, bot, username)
            return@onUpdate
        }
        when(val newState = update.data.newChatMemberState) {
            is AdministratorChatMember -> {
                channelService.activateChannel(chatId, chat.title)
                val username = newState.user.username?.username
                    ?: throw IllegalStateException("user has no username")
                userFlowsManager.initBotSetupFlow(chat, bot, username)
            }
            is LeftChatMember, is BannedChatMember -> {
                channelService.deactivate(chatId)
                userFlowsManager.stopFlowsForChat(chatId)
            }
            else -> logger.info("unexpected new member state: $newState. Update ignored")
        }
    }

    @Bean
    fun onCallbackQuery(
        userFlowsManager: UserFlowsManager
    ) = onUpdate<CallbackQueryUpdate> { update ->
        val chatId = update.data.message?.chat?.id?.chatId
            ?: throw IllegalStateException("callback query has no chat")
        val callback = update.data.asDataCallbackQuery()
            ?: throw IllegalStateException("callback query has no data")
        userFlowsManager.handle(chatId, UserAction.CallbackQueryAction(callback))
    }

    @Bean
    // TODO add on edit handle and on delete handlers
    fun onChannelPost(
        mirroringService: MirroringService,
        channelService: ChannelService,
        userFlowsManager: UserFlowsManager
    ) = onUpdate<ChannelPostUpdate> { update ->
        val chatEvent = update.data.asChannelEventMessage()
        if (chatEvent != null) {
            val newChatTitle = chatEvent.chatEvent.asNewChatTitle()
            if (newChatTitle != null) {
                channelService.updateTitle(chatEvent.chat.id.chatId, newChatTitle.title)
                return@onUpdate
            }
        }

        val chat = update.data.chat.asChannelChat() ?: error("chat is not channel")
        val chatId = chat.id.chatId
        if (mirroringService.configuredFor(chatId)) {
            logger.info("mirroring for channel ${chat.id} is configured. Try to mirror message")
            mirroringService.mirror(update.data, bot)
            return@onUpdate
        }

        val handled = userFlowsManager.handle(chatId, UserAction.MessageAction(update.data))
        if (!handled) {
            val username = update.data.from?.username?.username
                ?: error("message has no sender")
            userFlowsManager.initBotSetupFlow(chat, bot, username)
        }
    }

    class HandlerContext(
        val bot: TelegramBot,
        val logger: Logger
    )

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

    private final inline fun <reified T: Update> onUpdate(crossinline handler: suspend HandlerContext.(T) -> Unit) = object : TypedUpdateHandler<T> {
        override fun checkType(update: Update): T? = update as? T
        override suspend fun handle(update: T, bot: TelegramBot) {
            val context = HandlerContext(bot, LoggerFactory.getLogger(T::class.java))
            try {
                context.handler(update)
            } catch (e: Throwable) {
                context.logger.error("error while handling update", e)
            }
        }
    }
}