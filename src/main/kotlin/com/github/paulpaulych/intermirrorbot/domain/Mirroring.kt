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

    init {
        if (targets.any { it.channelId == srcChannelId }) {
            throw DomainException("cannot mirror to the same channel")
        }
    }

    fun addTarget(channelId: UUID, lang: Language): Mirroring {
        if (targets.any { it.channelId == channelId }) {
            throw DomainException("mirroring target with channelId=$channelId already exists")
        }
        if (targets.any { it.lang == lang }) {
            throw DomainException("mirroring target with lang=$lang already exists")
        }
        return this.copy(targets = targets + MirroringTarget(channelId, lang))
    }

    fun removeMirroringTarget(channelId: UUID): Mirroring {
        return this.copy(targets = targets.filter { it.channelId != channelId })
    }
}

data class MirroringTarget(
    val channelId: UUID,
    val lang: Language
)

