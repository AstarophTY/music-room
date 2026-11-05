package com.musicroom.app.player

/**
 * Plays one MP3 file inside the app, for the songs uploaded to Music Room (Spotify plays the others on the user's
 * devices). [load] replaces the previous sound and waits paused at the start; [onEnded] runs (on any thread) once it
 * plays to the end.
 */
expect class LocalAudioPlayer() {
    suspend fun load(bytes: ByteArray, onEnded: () -> Unit)

    fun play()

    fun pause()

    fun seek(positionMs: Long)

    fun positionMs(): Long

    /** Stops and frees the sound. */
    fun release()
}
