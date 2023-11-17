package com.github.paulpaulych.intermirrorbot.domain

enum class Language(
    val readableName: String,
    val readInThisLangMessage: String
) {
    RU("Russian", "Читать на русском"),
    EN("English", "Read in English"),
    ES("Spanish", "Leer en Español");

    fun originalPostLinkText(): Pair<String, String> = when (this) {
        RU -> Pair("Оригинальный пост", "здесь")
        EN -> Pair("Original post is", "here")
        ES -> Pair("La publicación original está", "aquí")
    }

    fun translatedBy(): String = when (this) {
        RU -> "Переведено с помощью"
        EN -> "Translated by"
        ES -> "Traducido por"
    }

    fun and(): String = when (this) {
        RU -> "и"
        EN -> "and"
        ES -> "y"
    }
}