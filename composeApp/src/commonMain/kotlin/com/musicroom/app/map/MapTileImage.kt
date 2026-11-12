package com.musicroom.app.map

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.musicroom.app.common.ImageCache
import com.musicroom.shared.MusicRoomApi
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * One tile of the map, downloaded once then kept with the covers in [ImageCache]: panning back over a
 * tile already seen draws it again without asking for it. Nothing shows until it arrives, which leaves
 * the background of the map rather than a flash of grey.
 */
@Composable
fun MapTileImage(api: MusicRoomApi, tile: MapTile, modifier: Modifier = Modifier) {
    val url = tile.url
    var image by remember(url) { mutableStateOf(ImageCache[url]) }
    LaunchedEffect(url) {
        if (image != null) return@LaunchedEffect
        image = try {
            api.download(url, MapTile.HEADERS).decodeToImageBitmap().also { ImageCache[url] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
    image?.let { Image(it, contentDescription = null, modifier = modifier, contentScale = ContentScale.FillBounds) }
}
