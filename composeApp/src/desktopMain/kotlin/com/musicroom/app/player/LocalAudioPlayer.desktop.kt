package com.musicroom.app.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/**
 * Java Sound: the MP3 file is decoded (mp3spi) to PCM once, then a thread writes it to the sound card. The position
 * is where that thread is in the PCM data, minus what waits in the sound card's buffer.
 */
actual class LocalAudioPlayer actual constructor() {
    private val lock = Object()
    private var pcm = ByteArray(0)
    private var format: AudioFormat? = null
    private var line: SourceDataLine? = null
    private var thread: Thread? = null
    private var onEnded: () -> Unit = {}

    // Read by the playback thread.
    @Volatile private var offset = 0
    @Volatile private var playing = false
    @Volatile private var running = false

    actual suspend fun load(bytes: ByteArray, onEnded: () -> Unit) {
        release()
        val (decoded, pcmFormat) = withContext(Dispatchers.IO) {
            AudioSystem.getAudioInputStream(BufferedInputStream(ByteArrayInputStream(bytes))).use { source ->
                val base = source.format
                val target = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, base.sampleRate, 16, base.channels, base.channels * 2, base.sampleRate, false)
                AudioSystem.getAudioInputStream(target, source).use { it.readBytes() } to target
            }
        }
        synchronized(lock) {
            pcm = decoded
            format = pcmFormat
            offset = 0
            this.onEnded = onEnded
        }
    }

    actual fun play() {
        synchronized(lock) {
            val pcmFormat = format ?: return
            if (offset >= pcm.size) offset = 0
            playing = true
            line?.start()
            if (thread == null) {
                running = true
                thread = Thread({ playLoop(pcmFormat) }, "music-room-audio").apply {
                    isDaemon = true
                    start()
                }
            }
        }
    }

    actual fun pause() {
        synchronized(lock) {
            playing = false
            line?.stop()
        }
    }

    actual fun seek(positionMs: Long) {
        synchronized(lock) {
            val pcmFormat = format ?: return
            val frame = (positionMs * pcmFormat.frameRate / 1000).toLong() * pcmFormat.frameSize
            offset = frame.coerceIn(0, pcm.size.toLong()).toInt()
            line?.flush()
        }
    }

    actual fun positionMs(): Long = synchronized(lock) {
        val pcmFormat = format ?: return 0
        val buffered = line?.let { it.bufferSize - it.available() } ?: 0
        val played = (offset - buffered).coerceAtLeast(0)
        (played / pcmFormat.frameSize * 1000L / pcmFormat.frameRate).toLong()
    }

    actual fun release() {
        val stopped = synchronized(lock) {
            running = false
            playing = false
            line?.let {
                it.stop()
                it.flush()
            }
            thread.also { thread = null }
        }
        stopped?.join(500)
        synchronized(lock) {
            line = null
            pcm = ByteArray(0)
            format = null
            offset = 0
        }
    }

    private fun playLoop(pcmFormat: AudioFormat) {
        val output = AudioSystem.getSourceDataLine(pcmFormat)
        output.open(pcmFormat)
        output.start()
        synchronized(lock) { line = output }
        val chunk = pcmFormat.frameSize * 1024
        while (running) {
            if (!playing) {
                Thread.sleep(20)
                continue
            }
            val (data, start, count) = synchronized(lock) {
                Triple(pcm, offset, minOf(chunk, pcm.size - offset))
            }
            if (count <= 0) {
                output.drain()
                val ended = synchronized(lock) {
                    // Still at the end (not moved meanwhile): the song is over.
                    (playing && offset >= pcm.size).also { if (it) playing = false }
                }
                if (ended) onEnded()
                continue
            }
            // Blocks while the sound card's buffer is full.
            val written = output.write(data, start, count)
            synchronized(lock) { if (offset == start) offset = start + written }
        }
        output.close()
    }
}
