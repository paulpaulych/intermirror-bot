package com.github.paulpaulych.intermirrorbot.domain

import java.util.*


data class Mirroring(
    val id: UUID,
    val srcChannelId: UUID,
    val targets: List<MirroringTarget>
) {
    companion object {
        fun create(srcChannelId: UUID): Mirroring {
            return Mirroring(UUID.randomUUID(), srcChannelId, listOf())
        }
    }

    fun addTarget(channelId: UUID): Mirroring {
        return this.copy(targets = targets + MirroringTarget(channelId))
    }

    fun removeMirroringTarget(channelId: UUID): Mirroring {
        return this.copy(targets = targets.filter { it.channelId != channelId })
    }
}

data class MirroringTarget(
    val channelId: UUID
)

