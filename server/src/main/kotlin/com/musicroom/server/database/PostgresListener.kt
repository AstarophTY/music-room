package com.musicroom.server.database

import com.musicroom.server.config.DatabaseConfig
import java.sql.DriverManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.postgresql.PGConnection
import org.slf4j.LoggerFactory

/** Receives the notifications of one PostgreSQL channel on a dedicated connection, reconnecting if it goes away. */
class PostgresListener(private val config: DatabaseConfig, private val channel: String) {
    private val log = LoggerFactory.getLogger(PostgresListener::class.java)

    /** Hands every payload to [deliver] until [scope] ends. */
    fun listen(scope: CoroutineScope, deliver: suspend (String) -> Unit): Job = scope.launch(Dispatchers.IO) {
        while (isActive) {
            try {
                DriverManager.getConnection(config.url, config.user, config.password).use { connection ->
                    connection.createStatement().use { it.execute("LISTEN $channel") }
                    val notifications = connection.unwrap(PGConnection::class.java)
                    while (isActive) {
                        notifications.getNotifications(POLL_MS)?.forEach { deliver(it.parameter) }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.warn("Listener on {} disconnected: {}", channel, e.message)
                delay(RECONNECT_MS)
            }
        }
    }

    private companion object {
        const val POLL_MS = 1000
        const val RECONNECT_MS = 2000L
    }
}
