package com.musicroom.app.common

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
import androidx.compose.ui.layout.ContentScale
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.decodeToImageBitmap

/** Image from a public URL (album cover), downloaded once then cached. A music note shows meanwhile or on error. */
@Composable
fun RemoteImage(api: MusicRoomApi, url: String?, modifier: Modifier = Modifier) {
    var image by remember(url) { mutableStateOf(url?.let { ImageCache[it] }) }
    LaunchedEffect(url) {
        if (url == null || image != null) return@LaunchedEffect
        image = try {
            api.download(url).decodeToImageBitmap().also { ImageCache[url] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
    Box(modifier.background(AppTheme.colors.surfaceSubtle), contentAlignment = Alignment.Center) {
        val loaded = image
        if (loaded != null) {
            Image(loaded, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Lucide.Music, contentDescription = null, tint = AppTheme.colors.textDisabled, modifier = Modifier.fillMaxSize(0.4f))
        }
    }
}
