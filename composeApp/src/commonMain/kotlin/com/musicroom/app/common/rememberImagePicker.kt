package com.musicroom.app.common

import androidx.compose.runtime.Composable

/**
 * Returns a function that opens the platform's photo picker. [onPicked] gets the image, or null if the user
 * cancelled. Returns null on platforms without a picker yet.
 */
@Composable
expect fun rememberImagePicker(onPicked: (PickedImage?) -> Unit): (() -> Unit)?
