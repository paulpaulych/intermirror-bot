package com.github.paulpaulych.intermirrorbot.service

import com.github.paulpaulych.intermirrorbot.dao.ChannelRepository
import com.github.paulpaulych.intermirrorbot.dao.MirroringRepository
import com.github.paulpaulych.intermirrorbot.domain.Mirroring
import com.github.paulpaulych.intermirrorbot.openai.OpenAiClient
import com.github.paulpaulych.intermirrorbot.openai.OpenAiMessage
import com.github.paulpaulych.intermirrorbot.openai.OpenAiRole
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.abstracts.Message
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MirroringService(
    private val channelRepository: ChannelRepository,
    private val mirroringRepository: MirroringRepository,
    private val openAiClient: OpenAiClient
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    suspend fun startMirroringFromChannel(chatId: Long) {
        val channel = channelRepository.getByChatId(chatId)
            ?: error("channel not found by chatId=$chatId")
        val mirroring = Mirroring.create(srcChannelId = channel.id)
        mirroringRepository.save(mirroring)
        logger.info("started mirroring from channel ${channel.title}, mirroringId=${mirroring.id}")
    }

    @Transactional
    suspend fun addTarget(srcChatId: Long, tgtChatId: Long) {
        val srcChannel = channelRepository.getByChatId(srcChatId)
            ?: error("channel not found by chatId=$srcChatId")
        val tgtChannel = channelRepository.getByChatId(tgtChatId)
            ?: error("channel not found by chatId=$tgtChatId")
        val mirroring = mirroringRepository.getBySrcChannelId(srcChannel.id)
            ?: error("mirroring not found for channel ${srcChannel.title}")
        mirroringRepository.save(mirroring.addTarget(tgtChannel.id))
        logger.info("added target ${tgtChannel.title} to mirroring ${mirroring.id}")
    }

    // TODO make it persistently async
    @Transactional
    suspend fun mirror(message: Message, bot: TelegramBot) {
        val srcChannel = channelRepository.getByChatId(message.chat.id.chatId)
            ?: error("channel not found by chatId=${message.chat.id.chatId}")
        val mirroring = mirroringRepository.getBySrcChannelId(srcChannel.id)
        if (mirroring == null) {
            logger.info("mirroring not configured for channel ${srcChannel.title}. Ignoring message.")
            return
        }
        val translated = message.text
            ?.takeIf { it.isNotBlank() }
            ?.let { translateText(it) }
            ?: ""
        mirroring.targets.forEach { tgt ->
            val tgtChannel = channelRepository.getById(tgt.channelId)
                ?: error("channel not found by chatId=${tgt.channelId}")
            bot.execute(SendTextMessage(
                chatId = ChatId(tgtChannel.chatId),
                text = translated
            ))
        }
        logger.info("mirrored message from ${srcChannel.title} to ${mirroring.targets.size} channels")
    }

    private suspend fun translateText(text: String): String {
        val response = openAiClient.getAssistantResponse(listOf(
            OpenAiMessage(OpenAiRole.SYSTEM, "Translate all user messages from Russian to English. Keep original formatting and style."),
            OpenAiMessage(OpenAiRole.USER, text),
        ))
        return response.message.content
    }
}