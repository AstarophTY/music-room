package com.musicroom.app.playlists

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.musicroom.app.common.ImageCache
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.decodeToImageBitmap

private val playlistFallbackCache = mutableMapOf<String, String>()

/**
 * Photo of a Music Room playlist, downloaded once per version then cached. Without a photo: the cover of its
 * first song ([fallbackUrl]), or fetched from its tracks, or a yellow tile with a music icon.
 */
@Composable
fun PlaylistCover(api: MusicRoomApi, playlistId: String, coverVersion: Long?, fallbackUrl: String?, modifier: Modifier = Modifier) {
    val key = coverVersion?.let { "playlist-cover:$playlistId:$it" }
    var photo by remember(key) { mutableStateOf(key?.let { ImageCache[it] }) }
    var resolvedFallback by remember(playlistId, fallbackUrl) {
        mutableStateOf(fallbackUrl ?: playlistFallbackCache[playlistId])
    }

    LaunchedEffect(key) {
        if (key == null || photo != null) return@LaunchedEffect
        photo = try {
            api.playlistCover(playlistId).decodeToImageBitmap().also { ImageCache[key] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    LaunchedEffect(playlistId, coverVersion, fallbackUrl) {
        if (coverVersion != null || resolvedFallback != null || playlistId.isBlank()) return@LaunchedEffect
        try {
            val detail = api.roomPlaylist(playlistId)
            val firstTrackCover = detail.tracks.firstOrNull()?.track?.imageUrl
            if (firstTrackCover != null) {
                playlistFallbackCache[playlistId] = firstTrackCover
                resolvedFallback = firstTrackCover
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    val loaded = photo
    val fallback = resolvedFallback
    when {
        loaded != null -> Image(loaded, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
        coverVersion == null && fallback != null -> RemoteImage(api, fallback, modifier)
        else -> {
            val colors = AppTheme.colors
            Box(
                modifier.background(Brush.linearGradient(listOf(colors.accent, colors.accentSubtleStrong))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.ListMusic, contentDescription = null, tint = colors.onAccent, modifier = Modifier.fillMaxSize(0.4f))
            }
        }
    }
}
