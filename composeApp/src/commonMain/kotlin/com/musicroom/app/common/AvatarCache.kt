package com.musicroom.app.common

import androidx.compose.ui.graphics.ImageBitmap

/** Profile photos already downloaded, keyed by "userId:version" (a new photo has a new version). */
object AvatarCache {
    private val images = mutableMapOf<String, ImageBitmap>()

    fun key(userId: String, version: Long) = "$userId:$version"

    operator fun get(key: String): ImageBitmap? = images[key]

    operator fun set(key: String, image: ImageBitmap) {
        images[key] = image
    }
}
