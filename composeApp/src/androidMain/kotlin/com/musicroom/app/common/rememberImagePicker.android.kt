package com.musicroom.app.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Android system photo picker: no storage permission needed. */
@Composable
actual fun rememberImagePicker(onPicked: (PickedImage?) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        onPicked(
            uri?.let {
                val bytes = context.contentResolver.openInputStream(it)?.use { input -> input.readBytes() }
                bytes?.let { data -> PickedImage(data, context.contentResolver.getType(it) ?: "image/jpeg") }
            },
        )
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}
