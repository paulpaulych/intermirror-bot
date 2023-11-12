package com.github.paulpaulych.intermirrorbot.dao

import com.github.paulpaulych.intermirrorbot.domain.Channel
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.text
import org.ktorm.schema.uuid
import java.util.*

interface ChannelEntity : Entity<ChannelEntity> {
    companion object : Entity.Factory<ChannelEntity>()

    val id: UUID
    val name: String
}

object ChannelTable : Table<ChannelEntity>("channel") {
    val id = uuid("id").primaryKey()
    val name = text("name")
}

class ChannelRepository(
    private val database: Database
) {

    fun save(channel: Channel) {
        if (get(channel.id) == null) {
            database.insert(ChannelTable) {
                set(it.id, channel.id)
                set(it.name, channel.name)
            }
        } else {
            update(channel)
        }
    }

    fun get(id: UUID): Channel? {
        return database
            .from(ChannelTable)
            .select()
            .where { ChannelTable.id eq id }
            .map(ChannelTable::createEntity)
            .map(::toChannel)
            .firstOrNull()
    }

    fun getByName(name: String): Channel? {
        return database
            .from(ChannelTable)
            .select()
            .where { ChannelTable.name eq name }
            .map(ChannelTable::createEntity)
            .map(::toChannel)
            .firstOrNull()
    }

    fun getAll(): List<Channel> {
        return database
            .from(ChannelTable)
            .select()
            .map(ChannelTable::createEntity)
            .map(::toChannel)
    }

    fun delete(id: UUID) {
        database.delete(ChannelTable) {
            it.id eq id
        }
    }

    fun update(channel: Channel) {
        database.update(ChannelTable) {
            set(it.name, channel.name)
            where {
                it.id eq channel.id
            }
        }
    }

    private fun toChannel(row: ChannelEntity): Channel {
        return Channel(
            id = row.id,
            name = row.name
        )
    }
}