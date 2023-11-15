package com.github.paulpaulych.intermirrorbot.dao

import com.github.paulpaulych.intermirrorbot.domain.Mirroring
import com.github.paulpaulych.intermirrorbot.domain.MirroringTarget
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
    val mirroringId: UUID
)

@Repository
class MirroringRepository(
    private val entityManager: EntityManager
) {

    fun save(mirroring: Mirroring) {
        entityManager.merge(MirroringEntity(
            id = mirroring.id,
            srcChannelId = mirroring.srcChannelId
        ))
        mirroring.targets.forEach { tgt ->
            entityManager.merge(MirroringTargetEntity(
                channelId = tgt.channelId,
                mirroringId = mirroring.id
            ))
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
            .singleResult
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
            channelId = entity.channelId
        )
    }
}