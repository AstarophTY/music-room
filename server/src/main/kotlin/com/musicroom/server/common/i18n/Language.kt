package com.musicroom.server.common.i18n

/** Languages the server writes emails in. */
enum class Language {
    EN,
    FR,
    ;

    companion object {
        /** Picks the first supported language of an Accept-Language header ("fr-FR,fr;q=0.9,en;q=0.8"). */
        fun fromAcceptLanguage(header: String?): Language = header.orEmpty()
            .split(',')
            .map { it.substringBefore(';').trim().substringBefore('-').lowercase() }
            .firstNotNullOfOrNull { tag -> entries.firstOrNull { it.name.lowercase() == tag } }
            ?: EN
    }
}
