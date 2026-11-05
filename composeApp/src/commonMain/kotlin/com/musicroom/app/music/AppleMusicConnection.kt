package com.musicroom.app.music

import androidx.compose.runtime.staticCompositionLocalOf

interface AppleMusicConnection {
    val available: Boolean
    val connected: Boolean
    suspend fun connect(developerToken: String): Boolean
    fun disconnect()

    companion object {
        val Local = staticCompositionLocalOf<AppleMusicConnection> { Unavailable }

        private object Unavailable : AppleMusicConnection {
            override val available = false
            override val connected = false
            override suspend fun connect(developerToken: String) = false
            override fun disconnect() = Unit
        }
    }
}
