package com.musicroom.server.database

import com.musicroom.server.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.Closeable

class DatabaseModule(private val dataSource: HikariDataSource) : Closeable {

    val database: Database = Database.connect(dataSource)

    suspend fun <T> query(block: suspend Transaction.() -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }

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
