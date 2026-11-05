package com.musicroom.app.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.document
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get

/** Browser file chooser (a hidden <input type="file">), limited to MP3 files. */
@Composable
actual fun rememberAudioPicker(onPicked: (PickedAudio?) -> Unit): (() -> Unit)? = remember(onPicked) {
    {
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = "audio/mpeg,.mp3"
        input.onchange = {
            val file = input.files?.get(0)
            if (file == null) {
                onPicked(null)
            } else {
                val reader = FileReader()
                reader.onload = {
                    val buffer = reader.result as ArrayBuffer
                    val bytes = Int8Array(buffer).let { array -> ByteArray(array.length) { index -> array[index] } }
                    onPicked(PickedAudio(bytes, file.name.substringBeforeLast('.')))
                }
                reader.readAsArrayBuffer(file)
            }
        }
        input.click()
    }
}
