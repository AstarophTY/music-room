package com.musicroom.app.player

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.Foundation.NSData
import platform.Foundation.create
import platform.darwin.NSObject

/** AVAudioPlayer, reading the file from memory. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class LocalAudioPlayer actual constructor() {
    private var player: AVAudioPlayer? = null

    // AVAudioPlayer keeps its delegate weakly: it is held here.
    private var listener: EndListener? = null

    actual suspend fun load(bytes: ByteArray, onEnded: () -> Unit) {
        release()
        if (bytes.isEmpty()) throw IllegalArgumentException("Empty audio file")
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, null)
        session.setActive(true, null)
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        val created = AVAudioPlayer(data = data, error = null)
        val endListener = EndListener(onEnded)
        created.delegate = endListener
        created.prepareToPlay()
        player = created
        listener = endListener
    }

    actual fun play() {
        player?.play()
    }

    actual fun pause() {
        player?.pause()
    }

    actual fun seek(positionMs: Long) {
        player?.currentTime = positionMs / 1000.0
    }

    actual fun positionMs(): Long = ((player?.currentTime ?: 0.0) * 1000).toLong()

    actual fun release() {
        player?.stop()
        player?.delegate = null
        player = null
        listener = null
    }

    private class EndListener(private val onEnded: () -> Unit) : NSObject(), AVAudioPlayerDelegateProtocol {
        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) = onEnded()
    }
}
