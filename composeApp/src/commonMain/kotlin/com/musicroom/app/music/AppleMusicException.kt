package com.musicroom.app.music

/**
 * An Apple Music failure coming from the device itself, which the server knows nothing about.
 * [whilePlaying] tells a song that would not play apart from an account that would not link.
 */
class AppleMusicException(
    message: String,
    val whilePlaying: Boolean = false,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
