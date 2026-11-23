package com.musicroom.server.database

import com.musicroom.server.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.Closeable

class DatabaseModule(private val dataSource: HikariDataSource) : Closeable {

    val database: Database = Database.connect(dataSource)

    suspend fun <T> query(block: suspend JdbcTransaction.() -> T): T =
        withContext(Dispatchers.IO) { suspendTransaction(db = database) { block() } }

    /** Sends a PostgreSQL notification on its own connection, committed right away. */
    suspend fun notify(channel: String, payload: String) = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT pg_notify(?, ?)").use { statement ->
                statement.setString(1, channel)
                statement.setString(2, payload)
                statement.execute()
            }
            connection.commit()
        }
    }

    fun isHealthy(): Boolean = runCatching {
        transaction(database) { exec("SELECT 1") { it.next() } }
    }.isSuccess

    override fun close() = dataSource.close()

    companion object {
        fun connect(config: DatabaseConfig): DatabaseModule {
            val dataSource = createDataSource(config)
            migrate(dataSource)
            return DatabaseModule(dataSource)
        }

        fun createDataSource(config: DatabaseConfig) = HikariDataSource(HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = config.maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            validate()
        })

        fun migrate(dataSource: HikariDataSource) {
            Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate()
        }
    }
}
