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
import dev.inmo.tgbotapi.types.message.HTMLParseMode
import dev.inmo.tgbotapi.utils.row

object BotSetupFlow {

    fun initialState(
        bot: TelegramBot,
        mirroringService: MirroringService,
        channelService: ChannelService,
        chat: ChannelChat,
        initiatorUsername: String
    ): UserInteractionState {
        return TargetOrSourceChooseState(
            UserInteractionContext(bot, mirroringService, channelService, chat.id.chatId, chat.title, initiatorUsername),
        )
    }
}

private data class UserInteractionContext(
    val bot: TelegramBot,
    val mirroringService: MirroringService,
    val channelService: ChannelService,
    val chatId: Long,
    val channelTitle: String,
    val initiatorUsername: String
)

private class TargetOrSourceChooseState(
    private val ctx: UserInteractionContext
): UserInteractionState {
    override suspend fun init(): State<UserAction> {
        ctx.bot.sendMessage(ChatId(ctx.chatId),
            """
                <b>Hi, ${ctx.initiatorUsername}!</b>
                
                I am IntermirrorBot and I can duplicate your channel in another language.
                First of all, you need to create your original channel and couple of destination channels.
                You can call them, for example, like here:
                
                 - <i>My awesome channel</i>
                 - <i>My awesome channel - RU</i>
                 - <i>My awesome channel - ES</i>               
                
                Would you like use this channel as original one or as translated copy?
            """.trimIndent(),
            replyMarkup = inlineKeyboard {
                row { +dataInlineButton("It's my original channel", "source") }
                row { +dataInlineButton("I want see translated copy here", "destination") }
            }
        )
        return this
    }

    override suspend fun handle(action: UserAction): State<UserAction>? {
        val callback = action.requireCallback()
        return when (callback.data) {
            "source" -> {
                ctx.mirroringService.startMirroringFromChannel(ctx.chatId)
                ctx.bot.sendMessage(ChatId(ctx.chatId), "This channel successfully registered as mirroring source")
                TargetChannelChooseState(ctx, ctx.chatId)
            }
            "destination" -> {
                ctx.bot.sendMessage(ChatId(ctx.chatId),
                    """
                        This channel  successfully registered as mirroring destination.
                        Please, go to a source channel and continue configuration there
                    """.trimIndent()
                )
                null
            }
            else -> {
                ctx.bot.sendMessage(ChatId(ctx.chatId), "Please, choose one of the options")
                this
            }
        }
    }
}

private class TargetChannelChooseState(
    private val ctx: UserInteractionContext,
    private val chatId: Long
): UserInteractionState {
    override suspend fun init(): State<UserAction> {
        ctx.bot.sendMessage(ChatId(chatId), "Input destination channel id")
        return this
    }

    override suspend fun handle(action: UserAction): State<UserAction> {
        return when(action) {
            is UserAction.CallbackQueryAction -> {
                val destChatId = action.callback.data.toLongOrNull()
                    ?: error("callback data is not a chatId")
                ConfirmTargetChannelState(ctx, chatId, destChatId)
            }
            is UserAction.MessageAction -> {
                val text = action.text
                if (text == null) {
                    ctx.bot.sendMessage(ChatId(chatId), "Please, input destination channel id")
                    return this
                }
                val matchedChannels = ctx.channelService.findByString(text)
                    .filter { it.chatId == ctx.chatId }
                return when (matchedChannels.size) {
                    0 -> {
                        ctx.bot.sendMessage(ChatId(chatId), "No channels found. Consider adding this bot as admin to the channel")
                        this
                    }
                    1 -> {
                        val matchedChannelState = matchedChannels.first().chatId
                        ConfirmTargetChannelState(ctx, chatId, matchedChannelState)
                    }
                    else -> {
                        ctx.bot.sendMessage(ChatId(chatId),
                            text = "Choose one of the channels: \n\n ${matchedChannels.joinToString("\n") { "${it.id} ${it.title}" }}",
                            replyMarkup = ReplyKeyboardMarkup(
                                matchedChannels.map { listOf(SimpleKeyboardButton("${it.id}")) }
                            )
                        )
                        this
                    }
                }
            }
        }
    }
}

private class ConfirmTargetChannelState(
    private val ctx: UserInteractionContext,
    private val chatId: Long,
    private val destChatId: Long
): UserInteractionState {
    override suspend fun init(): State<UserAction> {
        val destChannel = ctx.channelService.findByChatId(destChatId)
            ?: error("channel not found by chatId=$destChatId")
        ctx.bot.sendMessage(ChatId(chatId),
            text = "Do you want to translate all content of current channel to <b>${destChannel.title}</b>?",
            parseMode = HTMLParseMode,
            replyMarkup = inlineKeyboard {
                row {
                    add(dataInlineButton("Yes", "Yes"))
                    add(dataInlineButton("No", "No"))
                }
            }
        )
        return this
    }

    override suspend fun handle(action: UserAction): State<UserAction> {
        val callback = action.requireCallback()
        return when (callback.data) {
            "Yes" -> {
                ctx.bot.sendMessage(ChatId(chatId), "This channel successfully registered as mirroring destination")
                TargetLangChooseState(ctx, chatId, destChatId)
            }
            "No" -> {
                ctx.bot.sendMessage(ChatId(chatId), "Please, choose one of the options")
                this
            }
            else -> {
                ctx.bot.sendMessage(ChatId(chatId), "Please, choose one of the options")
                this
            }
        }
    }
}

private class TargetLangChooseState(
    private val ctx: UserInteractionContext,
    private val chatId: Long,
    private val destChatId: Long
): UserInteractionState {
    override suspend fun init(): State<UserAction> {
        ctx.bot.sendMessage(ChatId(chatId),
            text = "Now, choose the destination language:",
            replyMarkup = inlineKeyboard {
                Language.values().forEach { lang ->
                    row {
                        +dataInlineButton(lang.readableName, lang.name)
                    }
                }
            }
        )
        return this
    }

    override suspend fun handle(action: UserAction): State<UserAction>? {
        val callback = action.requireCallback()
        val lang = Language.valueOf(callback.data)
        ctx.mirroringService.addTarget(ctx.chatId, destChatId, lang)
        ctx.bot.sendMessage(ChatId(chatId), "This channel successfully registered as mirroring destination")
        return null
    }
}