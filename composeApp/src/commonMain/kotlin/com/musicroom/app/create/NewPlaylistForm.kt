package com.musicroom.app.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.musicroom.app.common.PickedImage
import com.musicroom.app.common.UiText
import com.musicroom.app.common.rememberImagePicker
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.playlist_create_button
import com.musicroom.app.generated.resources.playlist_name_label
import com.musicroom.app.generated.resources.playlist_photo_add
import com.musicroom.app.generated.resources.playlist_photo_change
import com.musicroom.app.generated.resources.playlists_create
import com.musicroom.app.playlists.PlaylistCover
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.PlaylistValidation
import com.musicroom.shared.dto.CreatePlaylistRequest
import com.musicroom.shared.dto.PlaylistResponse
import com.musicroom.shared.dto.PlaylistEditLicense
import com.musicroom.shared.dto.PlaylistVisibility
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource

/** Name, rights and optional photo of a new playlist. The photo is uploaded right after the playlist is created. */
@Composable
fun NewPlaylistForm(api: MusicRoomApi, onCreated: (PlaylistResponse) -> Unit) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf(PlaylistVisibility.PUBLIC) }
    var license by remember { mutableStateOf(PlaylistEditLicense.EVERYONE) }
    var photo by remember { mutableStateOf<PickedImage?>(null) }
    val preview = remember(photo) { photo?.let { runCatching { it.bytes.decodeToImageBitmap() }.getOrNull() } }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }
    val pickImage = rememberImagePicker { picked -> if (picked != null) photo = picked }

    fun submit() {
        PlaylistValidation.nameError(name)?.let {
            error = UiText.of(it)
            return
        }
        busy = true
        error = null
        scope.launch {
            try {
                val created = api.createPlaylist(CreatePlaylistRequest(name.trim(), visibility, license))
                val picked = photo
                // The playlist exists even if the photo upload fails: it can be added again from the playlist.
                val coverVersion = picked?.let {
                    runCatching { api.uploadPlaylistCover(created.id, it.bytes, ContentType.parse(it.mimeType)).playlist.coverVersion }.getOrNull()
                }
                onCreated(created.copy(coverVersion = coverVersion ?: created.coverVersion))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
                busy = false
            }
        }
    }

    Text(stringResource(Res.string.playlists_create), style = AppTheme.typography.title, color = colors.textPrimary)

    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
        val coverModifier = Modifier.size(96.dp).clip(AppShapes.large)
            .then(if (pickImage != null) Modifier.pressable(onClick = pickImage) else Modifier)
        Box(coverModifier) {
            if (preview != null) {
                Image(preview, contentDescription = null, modifier = Modifier.size(96.dp), contentScale = ContentScale.Crop)
            } else {
                PlaylistCover(api, playlistId = "", coverVersion = null, fallbackUrl = null, modifier = Modifier.size(96.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            AppTextField(name, { name = it }, stringResource(Res.string.playlist_name_label), Modifier.fillMaxWidth(), enabled = !busy, leadingIcon = Lucide.ListMusic)
            if (pickImage != null) {
                AppButton(
                    stringResource(if (photo == null) Res.string.playlist_photo_add else Res.string.playlist_photo_change),
                    pickImage,
                    enabled = !busy,
                    variant = AppButtonVariant.Ghost,
                    icon = Lucide.ImagePlus,
                )
            }
        }
    }

    PlaylistRightsOptions(visibility, license, { visibility = it }, { license = it }, enabled = !busy)

    AnimatedVisibility(error != null) {
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
    }
    AppButton(stringResource(Res.string.playlist_create_button), ::submit, Modifier.fillMaxWidth(), enabled = !busy)
}
