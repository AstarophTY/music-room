package com.musicroom.app.map

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import com.musicroom.app.common.ImageCache
import com.musicroom.shared.MusicRoomApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.time.Duration.Companion.seconds

/**
 * One tile of the map, downloaded once then kept with the covers in [ImageCache]: panning back over a
 * tile already seen draws it again without asking for it. Nothing shows until it arrives, which leaves
 * the background of the map rather than a flash of grey.
 *
 * A tile the server turns away (it throttles bursts) is asked for again a moment later: without that,
 * the hole it leaves stays until the map moves, since each tile is only fetched once per place.
 */
@Composable
fun MapTileImage(api: MusicRoomApi, tile: MapTile, tint: ColorFilter, modifier: Modifier = Modifier) {
    val attempts = 3
    val retryDelay = 2.seconds
    val url = tile.url
    var image by remember(url) { mutableStateOf(ImageCache[url]) }
    LaunchedEffect(url) {
        repeat(attempts) { attempt ->
            if (image != null) return@LaunchedEffect
            if (attempt > 0) delay(retryDelay)
            image = try {
                api.download(url, MapTile.HEADERS).decodeToImageBitmap().also { ImageCache[url] = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }
    image?.let {
        Image(it, contentDescription = null, modifier = modifier, contentScale = ContentScale.FillBounds, colorFilter = tint, filterQuality = FilterQuality.High)
    }
}
