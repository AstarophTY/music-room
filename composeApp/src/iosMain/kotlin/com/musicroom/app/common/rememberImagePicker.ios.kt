package com.musicroom.app.common

import androidx.compose.runtime.Composable

/** Not wired on iOS yet (needs PHPickerViewController): the app hides the photo button. */
@Composable
actual fun rememberImagePicker(onPicked: (PickedImage?) -> Unit): (() -> Unit)? = null
