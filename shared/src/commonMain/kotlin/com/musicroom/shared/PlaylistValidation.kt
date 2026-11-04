package com.musicroom.shared

/** Playlist input rules shared by the app (instant feedback) and the server (source of truth). */
object PlaylistValidation {
    const val NAME_MAX_LENGTH = 64

    /** Songs per playlist. */
    const val MAX_TRACKS = 500

    fun nameError(name: String): ValidationError? = when {
        name.isBlank() -> ValidationError.PLAYLIST_NAME_REQUIRED
        name.trim().length > NAME_MAX_LENGTH -> ValidationError.PLAYLIST_NAME_TOO_LONG
        else -> null
    }
}
