package com.github.paulpaulych.intermirrorbot.bot.userflows

import com.github.paulpaulych.intermirrorbot.bot.userflows.UserAction.CallbackQueryAction
import dev.inmo.tgbotapi.extensions.utils.asCommonMessage
import dev.inmo.tgbotapi.extensions.utils.asTextContent
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.message
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.queries.callback.CallbackQuery
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

typealias UserInteractionFlow = StateMachine<UserAction>

typealias UserInteractionState = State<UserAction>

sealed interface UserAction {

    data class MessageAction(val message: Message): UserAction

    data class CallbackQueryAction(val callback: DataCallbackQuery): UserAction
}

val UserAction.MessageAction.chatId: Long
    get() = message.chat.id.chatId

val UserAction.MessageAction.text: String?
    get() = message.asCommonMessage()?.content?.asTextContent()?.text?.takeIf { it.isNotEmpty() }

val UserAction.CallbackQueryAction.chatId: Long
    get() = callback.message?.chat?.id?.chatId ?: throw IllegalStateException("callback query has no chat")

fun UserAction.requireMessage(): Message {
    return (this as? UserAction.MessageAction)?.message ?: throw IllegalStateException("user action is not a message")
}

fun UserAction.requireCallback(): DataCallbackQuery {
    return (this as? CallbackQueryAction)?.callback ?: throw IllegalStateException("user action is not a callback")
}