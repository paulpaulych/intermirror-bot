package com.github.paulpaulych.intermirrorbot.service

import com.github.paulpaulych.intermirrorbot.dao.ChannelRepository
import com.github.paulpaulych.intermirrorbot.domain.Channel

class ChannelService(
    private val channelRepository: ChannelRepository
) {
    fun createChannel(name: String) {
        val channel = Channel.create(name)
        channelRepository.save(channel)
    }

    fun getChannel(name: String): Channel? {
        return channelRepository.getByName(name)
    }
}