package com.musicroom.app.player

import android.media.AudioAttributes
import android.media.MediaDataSource
import android.media.MediaPlayer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Android's MediaPlayer, reading the file from memory. */
actual class LocalAudioPlayer actual constructor() {
    private var player: MediaPlayer? = null

    actual suspend fun load(bytes: ByteArray, onEnded: () -> Unit) {
        release()
        val created = MediaPlayer()
        created.setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
        )
        created.setDataSource(BytesSource(bytes))
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                created.setOnPreparedListener { if (continuation.isActive) continuation.resume(Unit) }
                created.setOnErrorListener { _, what, _ ->
                    if (continuation.isActive) continuation.resumeWithException(IllegalStateException("Audio error $what"))
                    true
                }
                created.prepareAsync()
            }
        } catch (e: Throwable) {
            created.release()
            throw e
        }
        created.setOnCompletionListener { onEnded() }
        player = created
    }

    actual fun play() {
        player?.start()
    }

    actual fun pause() {
        player?.takeIf { it.isPlaying }?.pause()
    }

    actual fun seek(positionMs: Long) {
        player?.seekTo(positionMs.toInt())
    }

    actual fun positionMs(): Long = player?.currentPosition?.toLong() ?: 0

    actual fun release() {
        player?.release()
        player = null
    }

    /** The MP3 file kept in memory. */
    private class BytesSource(private val bytes: ByteArray) : MediaDataSource() {
        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            if (position >= bytes.size) return -1
            val count = minOf(size, bytes.size - position.toInt())
            bytes.copyInto(buffer, offset, position.toInt(), position.toInt() + count)
            return count
        }

        override fun getSize(): Long = bytes.size.toLong()

        override fun close() = Unit
    }
}
