package com.musicroom.app.player

import kotlinx.browser.document
import org.khronos.webgl.Int8Array
import org.khronos.webgl.set
import org.w3c.dom.HTMLAudioElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

/** An <audio> element playing the file from a blob URL. */
actual class LocalAudioPlayer actual constructor() {
    private var audio: HTMLAudioElement? = null
    private var url: String? = null

    actual suspend fun load(bytes: ByteArray, onEnded: () -> Unit) {
        release()
        val array = Int8Array(bytes.size)
        bytes.forEachIndexed { index, byte -> array[index] = byte }
        val parts = JsArray<JsAny?>()
        parts[0] = array
        val blobUrl = URL.createObjectURL(Blob(parts, BlobPropertyBag(type = "audio/mpeg")))
        val element = document.createElement("audio") as HTMLAudioElement
        element.src = blobUrl
        element.addEventListener("ended", { onEnded() })
        audio = element
        url = blobUrl
    }

    actual fun play() {
        audio?.play()
    }

    actual fun pause() {
        audio?.pause()
    }

    actual fun seek(positionMs: Long) {
        audio?.currentTime = positionMs / 1000.0
    }

    actual fun positionMs(): Long = ((audio?.currentTime ?: 0.0) * 1000).toLong()

    actual fun release() {
        audio?.let {
            it.pause()
            it.removeAttribute("src")
            it.load()
        }
        url?.let { URL.revokeObjectURL(it) }
        audio = null
        url = null
    }
}
