package com.musicroom.app.common

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Android's document picker, limited to MP3 files: no storage permission needed. */
@Composable
actual fun rememberAudioPicker(onPicked: (PickedAudio?) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onPicked(
            uri?.let {
                val bytes = context.contentResolver.openInputStream(it)?.use { input -> input.readBytes() }
                val name = context.contentResolver.query(it, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }.orEmpty()
                bytes?.let { data -> PickedAudio(data, name.substringBeforeLast('.')) }
            },
        )
    }
    return { launcher.launch("audio/mpeg") }
}
