package com.musicroom.server.common.mail

fun interface EmailSender {
    /** Sends a plain-text email. Throws if the SMTP server refuses it or cannot be reached. */
    suspend fun send(to: String, subject: String, body: String)
}
