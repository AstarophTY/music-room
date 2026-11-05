package com.musicroom.app.common

import androidx.compose.runtime.Composable

/** Not wired on iOS yet (needs UIDocumentPickerViewController): the app hides the upload button. */
@Composable
actual fun rememberAudioPicker(onPicked: (PickedAudio?) -> Unit): (() -> Unit)? = null
