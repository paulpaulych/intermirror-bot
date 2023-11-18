package com.github.paulpaulych.intermirrorbot.bot.userflows

import com.github.paulpaulych.intermirrorbot.bot.userflows.flows.BotSetupFlow
import com.github.paulpaulych.intermirrorbot.bot.userflows.flows.UserInstructionsFlow
import com.github.paulpaulych.intermirrorbot.core.service.ChannelService
import com.github.paulpaulych.intermirrorbot.core.service.MirroringService
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.types.chat.ChannelChat
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class UserFlowsManager(
    private val mirroringService: MirroringService,
    private val channelService: ChannelService
) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val flowsByChats = ConcurrentHashMap<Long, UserInteractionFlow>()

    suspend fun handle(chatId: Long, action: UserAction): Boolean {
        val flow = flowsByChats[chatId]
            ?: return false
        flow.handle(action)
        return true
    }

    suspend fun initBotSetupFlow(chat: ChannelChat, bot: TelegramBot, initiatorUsername: String) {
        val initialState = BotSetupFlow.initialState(bot, mirroringService, channelService, chat, initiatorUsername)
        when(val res = UserInteractionFlow.init(initialState)) {
            is StateMachine.StateMachineStartResult.Finished -> {
                logger.info("bot setup flow finished")
                return
            }
            is StateMachine.StateMachineStartResult.Success -> {
                logger.info("bot setup flow started")
                this.flowsByChats[chat.id.chatId] = res.stateMachine
            }
        }
    }

    suspend fun initUserInstructionsFlow(chatId: Long, bot: TelegramBot, initiatorUsername: String) {
        val initialState = UserInstructionsFlow.initialState(bot, chatId, initiatorUsername)
        when(val res = UserInteractionFlow.init(initialState)) {
            is StateMachine.StateMachineStartResult.Finished -> {
                logger.info("user instructions flow finished")
                return
            }
            is StateMachine.StateMachineStartResult.Success -> {
                logger.info("user instructions flow started")
                this.flowsByChats[chatId] = res.stateMachine
            }
        }
    }

    suspend fun stopFlowsForChat(chatId: Long) {
        flowsByChats.remove(chatId)
    }
}