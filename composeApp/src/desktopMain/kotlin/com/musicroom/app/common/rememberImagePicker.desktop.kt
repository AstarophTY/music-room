package com.musicroom.app.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/** Native file dialog of the OS, limited to JPEG and PNG files. */
@Composable
actual fun rememberImagePicker(onPicked: (PickedImage?) -> Unit): (() -> Unit)? = remember(onPicked) {
    {
        val dialog = FileDialog(null as Frame?, "", FileDialog.LOAD).apply {
            setFilenameFilter { _, name -> name.lowercase().substringAfterLast('.') in setOf("jpg", "jpeg", "png") }
            // Windows ignores the filter above and uses this pattern instead.
            if (System.getProperty("os.name").startsWith("Windows")) file = "*.jpg;*.jpeg;*.png"
            isVisible = true
        }
        val name = dialog.file
        onPicked(
            name?.let {
                val file = File(dialog.directory, it)
                PickedImage(file.readBytes(), if (it.lowercase().endsWith(".png")) "image/png" else "image/jpeg")
            },
        )
    }
}
