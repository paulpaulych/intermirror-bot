package com.github.paulpaulych.intermirrorbot.dao

import com.github.paulpaulych.intermirrorbot.domain.TgChannel
import com.github.paulpaulych.intermirrorbot.domain.TgChannel.ChannelStatus
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.Entity
import org.ktorm.schema.*
import org.springframework.stereotype.Repository
import java.util.*


@Repository
class ChannelRepository(
    private val database: Database
) {

    private interface ChannelEntity : Entity<ChannelEntity> {
        companion object : Entity.Factory<ChannelEntity>()

        val id: UUID
        val chatId: Long
        val title: String
        val status: ChannelStatus
    }

    private object ChannelTable : Table<ChannelEntity>("channel") {
        val id = uuid("id").primaryKey()
        val chatId = long("chat_id")
        val title = text("title")
        val status = enum<ChannelStatus>("status")
    }

    fun save(channel: TgChannel) {
        if (get(channel.id) == null) {
            database.insert(ChannelTable) {
                set(it.id, channel.id)
                set(it.title, channel.title)
                set(it.chatId, channel.chatId)
                set(it.status, channel.status)
            }
        } else {
            update(channel)
        }
    }

    fun get(id: UUID): TgChannel? {
        return database
            .from(ChannelTable)
            .select()
            .where { ChannelTable.id eq id }
            .map(ChannelTable::createEntity)
            .map(::toChannel)
            .firstOrNull()
    }

    fun getByChatId(chatId: Long): TgChannel? {
        return database
            .from(ChannelTable)
            .select()
            .where { ChannelTable.chatId eq chatId }
            .map { it ->
                TgChannel(
                    id = it[ChannelTable.id]!!,
                    chatId = it[ChannelTable.chatId]!!,
                    title = it[ChannelTable.title]!!,
                    status = it[ChannelTable.status]!!
                )
            }
//            .map(ChannelTable::createEntity)
//            .map(::toChannel)
            .firstOrNull()
    }

    fun getAll(): List<TgChannel> {
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

    fun update(channel: TgChannel) {
        database.update(ChannelTable) {
            set(it.title, channel.title)
            set(it.status, channel.status)
            set(it.chatId, channel.chatId)
            where {
                it.id eq channel.id
            }
        }
    }

    private fun toChannel(row: ChannelEntity): TgChannel {
        return TgChannel(
            id = row.id,
            chatId = row.chatId,
            title = row.title,
            status = row.status
        )
    }
}