package com.github.paulpaulych.intermirrorbot.core.service

import com.github.paulpaulych.intermirrorbot.core.dao.ChannelRepository
import com.github.paulpaulych.intermirrorbot.core.domain.TgChannel
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
    suspend fun findByString(criteria: String): List<TgChannel> {
        return criteria.toLongOrNull()
            ?.let { listOfNotNull(channelRepository.getByChatId(it)) }
            ?: channelRepository.getByChatTitle(criteria)
    }

    @Transactional
    suspend fun findByChatId(chatId: Long): TgChannel? {
        return channelRepository.getByChatId(chatId)
    }

    @Transactional
    suspend fun updateTitle(chatId: Long, title: String) {
        val channel = channelRepository.getByChatId(chatId)
        if (channel != null) {
            val newChannel = channel.copy(title = title)
            channelRepository.save(newChannel)
            logger.info("updated channel title chatId=${newChannel.chatId} new_title=${channel.title}")
        }
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