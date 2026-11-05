package com.musicroom.app.common

import androidx.compose.runtime.Composable

/**
 * Returns a function that opens the platform's file chooser for an MP3 file. [onPicked] gets the file, or null if
 * the user cancelled. Returns null on platforms without a chooser yet.
 */
@Composable
expect fun rememberAudioPicker(onPicked: (PickedAudio?) -> Unit): (() -> Unit)?
