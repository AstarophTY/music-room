package com.musicroom.app.common

import androidx.compose.ui.graphics.ImageBitmap

/** Downloaded images (album covers) keyed by URL. Keeps the most recent [MAX_ENTRIES]. */
object ImageCache {
    private const val MAX_ENTRIES = 200
    private val images = LinkedHashMap<String, ImageBitmap>()

    operator fun get(url: String): ImageBitmap? = images[url]

    operator fun set(url: String, image: ImageBitmap) {
        images.remove(url)
        images[url] = image
        if (images.size > MAX_ENTRIES) images.remove(images.keys.first())
    }
}
