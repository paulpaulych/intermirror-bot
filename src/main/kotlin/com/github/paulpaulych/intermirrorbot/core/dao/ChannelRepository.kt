package com.github.paulpaulych.intermirrorbot.core.dao

import com.github.paulpaulych.intermirrorbot.core.domain.TgChannel
import com.github.paulpaulych.intermirrorbot.core.domain.TgChannel.ChannelStatus
import jakarta.persistence.*
import org.springframework.stereotype.Repository
import java.util.*


@Entity
@Table(name = "channel")
data class ChannelEntity(
    @Id
    val id: UUID,
    @Column(name = "chat_id")
    val chatId: Long,
    val title: String,
    @Enumerated(value=EnumType.STRING)
    val status: ChannelStatus
)

@Repository
class ChannelRepository(
    private val entityManager: EntityManager
) {

    fun save(channel: TgChannel) {
        entityManager.merge(
            ChannelEntity(
            id = channel.id,
            chatId = channel.chatId,
            title = channel.title,
            status = channel.status
        )
        )
    }

    fun getById(id: UUID): TgChannel? {
        return entityManager.find(ChannelEntity::class.java, id)
            ?.let(::toChannel)
    }

    fun getByChatId(chatId: Long): TgChannel? {
        return entityManager
            .selectWithCriteria<ChannelEntity> { q ->
                q.where(equal(q.from(ChannelEntity::class.java).get<Long>("chatId"), chatId))
            }
            .resultList
            .firstOrNull()
            ?.let(::toChannel)
    }

    fun getByChatTitle(title: String): List<TgChannel> {
        return entityManager
            .selectWithCriteria<ChannelEntity> { q ->
                q.where(this.like(q.from(ChannelEntity::class.java).get("title"), "%$title%"))
            }
            .resultList
            .map(::toChannel)
    }

    fun toChannel(entity: ChannelEntity): TgChannel {
        return TgChannel(
            id = entity.id,
            chatId = entity.chatId,
            title = entity.title,
            status = entity.status
        )
    }
}