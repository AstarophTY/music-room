package com.musicroom.server.common.mail

import org.slf4j.LoggerFactory

/** Used when SMTP_HOST is empty (development): emails are written to the server logs instead of being sent. */
class LogEmailSender : EmailSender {
    private val log = LoggerFactory.getLogger(LogEmailSender::class.java)

    override suspend fun send(to: String, subject: String, body: String) {
        log.info("Email to {} (not sent, SMTP_HOST is empty)\nSubject: {}\n{}", to, subject, body)
    }
}
