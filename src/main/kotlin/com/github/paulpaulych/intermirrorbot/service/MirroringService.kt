package com.github.paulpaulych.intermirrorbot.service

import com.github.paulpaulych.intermirrorbot.dao.ChannelRepository
import com.github.paulpaulych.intermirrorbot.dao.MirroringRepository
import com.github.paulpaulych.intermirrorbot.domain.Language
import com.github.paulpaulych.intermirrorbot.domain.Mirroring
import com.github.paulpaulych.intermirrorbot.domain.MirroringTarget
import com.github.paulpaulych.intermirrorbot.openai.OpenAiClient
import com.github.paulpaulych.intermirrorbot.openai.OpenAiMessage
import com.github.paulpaulych.intermirrorbot.openai.OpenAiRole
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.utils.asContentMessage
import dev.inmo.tgbotapi.extensions.utils.asTextContent
import dev.inmo.tgbotapi.extensions.utils.formatting.messageLink
import dev.inmo.tgbotapi.extensions.utils.formatting.toHtmlTexts
import dev.inmo.tgbotapi.requests.edit.text.EditChatMessageText
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.HTMLParseMode
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.content.MessageContent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MirroringService(
    private val channelRepository: ChannelRepository,
    private val mirroringRepository: MirroringRepository,
    private val openAiClient: OpenAiClient
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    suspend fun startMirroringFromChannel(chatId: Long) {
        val channel = channelRepository.getByChatId(chatId)
            ?: error("channel not found by chatId=$chatId")
        val mirroring = Mirroring.create(srcChannelId = channel.id)
        mirroringRepository.save(mirroring)
        logger.info("started mirroring from channel ${channel.title}, mirroringId=${mirroring.id}")
    }

    @Transactional
    suspend fun addTarget(
        srcChatId: Long,
        tgtChatId: Long,
        lang: String
    ) {
        val language = Language.valueOf(lang)
        val srcChannel = channelRepository.getByChatId(srcChatId)
            ?: error("channel not found by chatId=$srcChatId")
        val tgtChannel = channelRepository.getByChatId(tgtChatId)
            ?: error("channel not found by chatId=$tgtChatId")
        val mirroring = mirroringRepository.getBySrcChannelId(srcChannel.id)
            ?: error("mirroring not found for channel ${srcChannel.title}")
        mirroringRepository.save(mirroring.addTarget(tgtChannel.id, language))
        logger.info("added target chatId=${tgtChatId} title=${tgtChannel.title} to mirroring ${mirroring.id}")
    }

    // TODO make it persistently async
    @Transactional
    suspend fun mirror(message: Message, bot: TelegramBot) {
        val srcChannel = channelRepository.getByChatId(message.chat.id.chatId)
            ?: error("channel not found by chatId=${message.chat.id.chatId}")
        val mirroring = mirroringRepository.getBySrcChannelId(srcChannel.id)
        if (mirroring == null) {
            logger.info("mirroring not configured for channel ${srcChannel.title}. Ignoring message.")
            return
        }

        val markdown = message.asContentMessage()?.content

        val newLinks = mirroring.targets.map { tgt ->
            createTranslatedCopy(tgt, message, bot, markdown)
        }

        editOriginalMessage(bot, message, newLinks)

        logger.info("mirrored message from ${srcChannel.title} to ${mirroring.targets.size} channels")
    }

    private suspend fun editOriginalMessage(
        bot: TelegramBot,
        message: Message,
        newLinks: List<Pair<String, Language>>
    ) {
        val originalText = message.asContentMessage()?.content
            ?.asTextContent()?.toHtmlTexts()
            ?.joinToString(",") ?: ""
        val resultTextForEdit = originalText + NEWLINE + NEWLINE + newLinks
            .joinToString("\n") { (newMsgLink, lang) ->
                buildLink(newMsgLink, lang.readInThisLangMessage)
            }
        bot.execute(EditChatMessageText(
            chatId = message.chat.id,
            messageId = message.messageId,
            text = resultTextForEdit,
            disableWebPagePreview = true,
            parseMode = HTMLParseMode
        ))
    }

    private suspend fun createTranslatedCopy(
        tgt: MirroringTarget,
        message: Message,
        bot: TelegramBot,
        content: MessageContent?
    ): Pair<String, Language> {
        val tgtChannel = channelRepository.getById(tgt.channelId)
            ?: error("channel not found by chatId=${tgt.channelId}")

        val content = content?.asTextContent()?.toHtmlTexts()
        val translated = content
            ?.joinToString(NEWLINE)
            ?.takeIf { it.isNotBlank() }
            ?.let { translateMarkdown(it, tgt.lang) }
            ?: ""
        val resultText = addFooter(message, translated)

        val result = bot.execute(SendTextMessage(
            chatId = ChatId(tgtChannel.chatId),
            text = resultText,
            parseMode = HTMLParseMode,
            disableWebPagePreview = true
        ))

        val link = result.messageLink
            ?: error("failed to get message link from result: $result")
        return Pair(link, tgt.lang)
    }

    private suspend fun translateMarkdown(text: String, language: Language): String {
        val response = openAiClient.getAssistantResponse(listOf(
            OpenAiMessage(OpenAiRole.SYSTEM,
                """
                    Translate all user messages to ${language.readableName}.
                    The incoming messages will be in HTML-like format.
                    Remember to escape characters that are not part of markdown markup with \\.
                    Keep original formatting and style.
                """.trimIndent()
            ),
            OpenAiMessage(OpenAiRole.USER, text),
        ))
        return response.message.content
    }

    private suspend fun addFooter(originalMsg: Message, text: String): String {
        val messageLink = originalMsg.messageLink
            ?: error("messageLink is null")
        val intermirrorLink = buildLink("https://github.com/paulpaulych/intermirror-bot", "Intermirror")
        val openaiLink = buildLink("https://openai.com", "OpenAI")
        val originalPostLink = buildLink(messageLink, "here")
        return text +
            NEWLINE + NEWLINE + "Original post is $originalPostLink" +
            NEWLINE + "Translated by $intermirrorLink and $openaiLink"
    }

    companion object {
        private const val NEWLINE = "\n"
    }

    private fun buildLink(url: String, text: String): String {
        return "<a href=\"$url\">$text</a>"
    }
}