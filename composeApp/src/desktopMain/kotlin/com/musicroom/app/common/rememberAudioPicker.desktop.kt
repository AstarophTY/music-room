package com.musicroom.app.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/** Native file dialog of the OS, limited to MP3 files. */
@Composable
actual fun rememberAudioPicker(onPicked: (PickedAudio?) -> Unit): (() -> Unit)? = remember(onPicked) {
    {
        val dialog = FileDialog(null as Frame?, "", FileDialog.LOAD).apply {
            setFilenameFilter { _, name -> name.lowercase().endsWith(".mp3") }
            // Windows ignores the filter above and uses this pattern instead.
            if (System.getProperty("os.name").startsWith("Windows")) file = "*.mp3"
            isVisible = true
        }
        val name = dialog.file
        onPicked(name?.let { PickedAudio(File(dialog.directory, it).readBytes(), it.substringBeforeLast('.')) })
    }
}
