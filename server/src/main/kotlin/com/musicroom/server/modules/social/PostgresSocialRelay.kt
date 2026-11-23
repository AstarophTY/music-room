package com.musicroom.server.modules.social

import com.musicroom.server.config.DatabaseConfig
import com.musicroom.server.database.DatabaseModule
import java.sql.DriverManager
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.postgresql.PGConnection
import org.slf4j.LoggerFactory

/**
 * Social events between services, over PostgreSQL LISTEN/NOTIFY. Delivery is best effort, like the WebSocket itself:
 * what is missed stays in the database and the app reads it from the API.
 */
class PostgresSocialRelay(
    private val database: DatabaseModule,
    private val config: DatabaseConfig,
) : SocialRelay {
    private val log = LoggerFactory.getLogger(PostgresSocialRelay::class.java)

    override suspend fun publish(userId: UUID, text: String) {
        val payload = "$userId$SEPARATOR$text"
        if (payload.toByteArray().size > MAX_PAYLOAD_BYTES) {
            log.warn("Social event for {} is too large to relay", userId)
            return
        }
        try {
            database.notify(CHANNEL, payload)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn("Could not relay a social event: {}", e.message)
        }
    }

    /** Hands every relayed event to [deliver] until [scope] ends, reconnecting if the database goes away. */
    fun listen(scope: CoroutineScope, deliver: suspend (UUID, String) -> Unit): Job = scope.launch(Dispatchers.IO) {
        while (isActive) {
            try {
                DriverManager.getConnection(config.url, config.user, config.password).use { connection ->
                    connection.createStatement().use { it.execute("LISTEN $CHANNEL") }
                    val notifications = connection.unwrap(PGConnection::class.java)
                    while (isActive) {
                        notifications.getNotifications(POLL_MS)?.forEach { notification ->
                            val userId = UUID.fromString(notification.parameter.substringBefore(SEPARATOR))
                            deliver(userId, notification.parameter.substringAfter(SEPARATOR))
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.warn("Social relay disconnected: {}", e.message)
                delay(RECONNECT_MS)
            }
        }
    }

    private companion object {
        const val CHANNEL = "social_events"
        const val SEPARATOR = '|'

        /** PostgreSQL refuses NOTIFY payloads of 8000 bytes or more. */
        const val MAX_PAYLOAD_BYTES = 7900
        const val POLL_MS = 1000
        const val RECONNECT_MS = 2000L
    }
}
