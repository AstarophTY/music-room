package com.musicroom.server.modules.social

import java.util.UUID

/** Carries an encoded social event to the service that holds the user's WebSockets. */
fun interface SocialRelay {
    suspend fun publish(userId: UUID, text: String)
}
