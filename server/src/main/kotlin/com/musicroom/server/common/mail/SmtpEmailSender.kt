package com.musicroom.server.common.mail

import com.musicroom.server.config.MailConfig
import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties

class SmtpEmailSender(private val config: MailConfig) : EmailSender {

    private val session: Session = Session.getInstance(
        Properties().apply {
            put("mail.smtp.host", config.host)
            put("mail.smtp.port", config.port.toString())
            put("mail.smtp.auth", config.user.isNotEmpty().toString())
            // Upgrades to TLS when the server offers it (real providers on port 587); Mailpit does not.
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.connectiontimeout", TIMEOUT_MS)
            put("mail.smtp.timeout", TIMEOUT_MS)
            put("mail.smtp.writetimeout", TIMEOUT_MS)
        },
        if (config.user.isEmpty()) {
            null
        } else {
            object : Authenticator() {
                override fun getPasswordAuthentication() = PasswordAuthentication(config.user, config.password)
            }
        },
    )

    override suspend fun send(to: String, subject: String, body: String) = withContext(Dispatchers.IO) {
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(config.from))
            setRecipient(Message.RecipientType.TO, InternetAddress(to))
            setSubject(subject, Charsets.UTF_8.name())
            setText(body, Charsets.UTF_8.name())
        }
        Transport.send(message)
    }

    private companion object {
        const val TIMEOUT_MS = "5000"
    }
}
