package com.github.paulpaulych.intermirrorbot.dao

import com.github.paulpaulych.intermirrorbot.domain.Mirroring
import com.github.paulpaulych.intermirrorbot.domain.MirroringTarget
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.uuid
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class MirroringRepository(
    private val database: Database
) {

    private interface MirroringEntity: Entity<MirroringEntity> {
        companion object: Entity.Factory<MirroringEntity>()

        val id: UUID
        val srcChannelId: UUID
    }

    private object MirroringTable: Table<MirroringEntity>("mirroring") {
        val id = uuid("id").primaryKey()
        val srcChannelId = uuid("src_channel_id")
    }

    private interface MirroringTargetEntity: Entity<MirroringTargetEntity> {
        companion object: Entity.Factory<MirroringTargetEntity>()

        val id: UUID
        val mirroringId: UUID
        val channelId: UUID
    }

    private object MirroringTargetTable: Table<MirroringTargetEntity>("mirroring_target") {
        val id = uuid("id").primaryKey()
        val mirroringId = uuid("mirroring_id")
        val channelId = uuid("channel_id")
    }

    fun save(mirroring: Mirroring) {
        if(getMirroringEntity(mirroring.id) == null) {
            database.insert(MirroringTable) {
                set(it.id, mirroring.id)
                set(it.srcChannelId, mirroring.srcChannelId)
            }
        } else {
            update(mirroring)
        }
    }

    private fun update(mirroring: Mirroring) {
        database.update(MirroringTable) {
            set(it.srcChannelId, mirroring.srcChannelId)
            where { it.id eq mirroring.id }
        }
        updateTargets(mirroring)
    }

    private fun updateTargets(mirroring: Mirroring) {
        database.delete(MirroringTargetTable) {
            it.mirroringId eq mirroring.id
        }
        mirroring.targets.forEach { target ->
            database.insert(MirroringTargetTable) {
                set(it.id, UUID.randomUUID())
                set(it.mirroringId, mirroring.id)
                set(it.channelId, target.channelId)
            }
        }
    }

    fun getMirroring(id: UUID): Mirroring? {
        return getMirroringEntity(id)?.let {
            Mirroring(
                id = it.id,
                srcChannelId = it.srcChannelId,
                targets = getMirroringTargets(id)
            )
        }
    }

    private fun getMirroringEntity(id: UUID): MirroringEntity? {
        return database
            .from(MirroringTable)
            .select()
            .where { MirroringTable.id eq id }
            .map(MirroringTable::createEntity)
            .firstOrNull()
    }

    private fun getMirroringTargets(id: UUID): List<MirroringTarget> {
        return database
            .from(MirroringTargetTable)
            .select()
            .where { MirroringTargetTable.mirroringId eq id }
            .map(MirroringTargetTable::createEntity)
            .map(::toMirroringTarget)
    }

    private fun toMirroringTarget(entity: MirroringTargetEntity): MirroringTarget {
        return MirroringTarget(
            channelId = entity.channelId
        )
    }
}