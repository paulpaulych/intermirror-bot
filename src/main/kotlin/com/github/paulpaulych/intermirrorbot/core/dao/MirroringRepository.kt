package com.github.paulpaulych.intermirrorbot.core.dao

import com.github.paulpaulych.intermirrorbot.core.domain.Language
import com.github.paulpaulych.intermirrorbot.core.domain.Mirroring
import com.github.paulpaulych.intermirrorbot.core.domain.MirroringTarget
import jakarta.persistence.*
import org.springframework.stereotype.Repository
import java.util.*


@Entity
@Table(name = "mirroring")
class MirroringEntity(
    @Id
    val id: UUID,
    @Column(name = "src_channel_id")
    val srcChannelId: UUID
)

@Entity
@Table(name = "mirroring_target")
class MirroringTargetEntity(
    @Id
    @Column(name = "channel_id")
    val channelId: UUID,
    @Column(name = "mirroring_id")
    val mirroringId: UUID,
    @Enumerated(value=EnumType.STRING)
    val lang: Language
)

@Repository
class MirroringRepository(
    private val entityManager: EntityManager
) {

    fun save(mirroring: Mirroring) {
        entityManager.merge(
            MirroringEntity(
            id = mirroring.id,
            srcChannelId = mirroring.srcChannelId
        )
        )
        mirroring.targets.forEach { tgt ->
            entityManager.merge(
                MirroringTargetEntity(
                channelId = tgt.channelId,
                mirroringId = mirroring.id,
                lang = tgt.lang
            )
            )
        }
    }

    fun getById(id: UUID): Mirroring? {
        return entityManager.find(MirroringEntity::class.java, id)
            ?.let(::toMirroring)
    }

    fun getBySrcChannelId(srcChannelId: UUID): Mirroring? {
        return entityManager
            .selectWithCriteria<MirroringEntity> { q ->
                q.where(equal(q.from(MirroringEntity::class.java).get<UUID>("srcChannelId"), srcChannelId))
            }
            .resultList
            .firstOrNull()
            ?.let(::toMirroring)
    }

    private fun toMirroring(entity: MirroringEntity): Mirroring {
        return Mirroring(
            id = entity.id,
            srcChannelId = entity.srcChannelId,
            targets = entityManager
                .selectWithCriteria<MirroringTargetEntity> { q ->
                    q.where(equal(q.from(MirroringTargetEntity::class.java).get<UUID>("mirroringId"), entity.id))
                }
                .resultList
                .map(::toMirroringTarget)
        )
    }

    private fun toMirroringTarget(entity: MirroringTargetEntity): MirroringTarget {
        return MirroringTarget(
            channelId = entity.channelId,
            lang = entity.lang
        )
    }
}