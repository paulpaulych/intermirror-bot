package com.github.paulpaulych.intermirrorbot.service

import com.github.paulpaulych.intermirrorbot.dao.ChannelRepository
import com.github.paulpaulych.intermirrorbot.domain.TgChannel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChannelService(
    private val channelRepository: ChannelRepository
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    suspend fun activateChannel(chatId: Long, title: String) {
        val channel = channelRepository.getByChatId(chatId)
            ?: TgChannel.create(chatId, title)
        channelRepository.save(channel)
        logger.info("activated channel $title")
    }

    @Transactional
    suspend fun deactivate(chatId: Long) {
        val channel = channelRepository.getByChatId(chatId)
        if (channel != null) {
            channelRepository.save(channel.deactivate())
            logger.info("deactivated channel ${channel.title}")
        }
    }
}