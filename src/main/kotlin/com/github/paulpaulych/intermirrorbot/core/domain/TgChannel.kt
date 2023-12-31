package com.github.paulpaulych.intermirrorbot.core.domain

import java.util.*

data class TgChannel(
    val id: UUID,
    val chatId: Long,
    val title: String,
    val status: ChannelStatus
) {

    enum class ChannelStatus {
        ACTIVE, INACTIVE
    }

    companion object {
        fun create(chatId: Long, title: String): TgChannel {
            return TgChannel(UUID.randomUUID(), chatId, title, ChannelStatus.ACTIVE)
        }
    }

    fun deactivate(): TgChannel = this.copy(status = ChannelStatus.INACTIVE)
}