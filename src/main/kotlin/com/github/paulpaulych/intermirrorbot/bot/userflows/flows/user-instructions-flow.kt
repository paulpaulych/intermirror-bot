package com.github.paulpaulych.intermirrorbot.bot.userflows.flows

import com.github.paulpaulych.intermirrorbot.bot.userflows.*
import com.github.paulpaulych.intermirrorbot.core.domain.Language
import com.github.paulpaulych.intermirrorbot.core.service.ChannelService
import com.github.paulpaulych.intermirrorbot.core.service.MirroringService
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.data
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.SimpleKeyboardButton
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.types.chat.ChannelChat
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.message.HTMLParseMode
import dev.inmo.tgbotapi.utils.row

object UserInstructionsFlow {

    fun initialState(
        bot: TelegramBot,
        chatId: Long,
        initiatorUsername: String
    ): UserInteractionState {
        return UserInstructionsInitialState(
            UserInstructionsContext(bot, chatId, initiatorUsername),
        )
    }
}

private data class UserInstructionsContext(
    val bot: TelegramBot,
    val chatId: Long,
    val initiatorUsername: String
)

private class UserInstructionsInitialState(
    private val ctx: UserInstructionsContext
): UserInteractionState {

    override suspend fun init(): State<UserAction>? {
        ctx.bot.sendMessage(ChatId(ctx.chatId),
            """
                <b>Hi, ${ctx.initiatorUsername}!</b>
                
                I am IntermirrorBot and I can duplicate your channel in another language.
                First of all, you need to create your original channel and couple of destination channels.
                You can call them, for example, like here:
                 - <i>My awesome channel</i>
                 - <i>My awesome channel - RU</i>
                 - <i>My awesome channel - ES</i>               
                
                Then, add me as admin to all of them, and will continue the configuration there.

                <a href="https://github.com/paulpaulych/intermirror-bot">Your Intermirror</a>
            """.trimIndent()
        )
        return null
    }

    override suspend fun handle(action: UserAction) = null
}
