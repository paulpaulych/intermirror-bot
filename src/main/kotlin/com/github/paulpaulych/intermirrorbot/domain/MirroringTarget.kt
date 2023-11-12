package com.github.paulpaulych.intermirrorbot.domain

import java.util.*


data class Channel(
    val id: UUID,
    val name: String
) {

    companion object {
        fun create(name: String): Channel {
            return Channel(UUID.randomUUID(), name)
        }
    }
}

data class Mirroring(
    val id: UUID,
    val srcChannelId: UUID,
    val targets: List<MirroringTarget>
)

data class MirroringTarget(
    val channelId: UUID
)

