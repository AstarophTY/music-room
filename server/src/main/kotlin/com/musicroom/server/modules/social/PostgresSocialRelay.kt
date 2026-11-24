package com.musicroom.server.modules.social

import com.musicroom.server.config.DatabaseConfig
import com.musicroom.server.database.DatabaseModule
import com.musicroom.server.database.PostgresListener
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.slf4j.LoggerFactory

/**
 * Social events between services, over PostgreSQL LISTEN/NOTIFY. Delivery is best effort, like the WebSocket itself:
 * what is missed stays in the database and the app reads it from the API.
 */
class PostgresSocialRelay(
    private val database: DatabaseModule,
    config: DatabaseConfig,
) : SocialRelay {
    private val log = LoggerFactory.getLogger(PostgresSocialRelay::class.java)
    private val listener = PostgresListener(config, CHANNEL)

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
    fun listen(scope: CoroutineScope, deliver: suspend (UUID, String) -> Unit): Job = listener.listen(scope) { payload ->
        deliver(UUID.fromString(payload.substringBefore(SEPARATOR)), payload.substringAfter(SEPARATOR))
    }

    private companion object {
        const val CHANNEL = "social_events"
        const val SEPARATOR = '|'

        /** PostgreSQL refuses NOTIFY payloads of 8000 bytes or more. */
        const val MAX_PAYLOAD_BYTES = 7900
    }
}
