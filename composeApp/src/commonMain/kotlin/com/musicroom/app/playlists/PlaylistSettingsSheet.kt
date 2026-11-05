package com.musicroom.app.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.ImageOff
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.musicroom.app.common.UiText
import com.musicroom.app.create.PlaylistTypeOption
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_cancel
import com.musicroom.app.generated.resources.common_save
import com.musicroom.app.generated.resources.playlist_delete
import com.musicroom.app.generated.resources.playlist_delete_confirm
import com.musicroom.app.generated.resources.playlist_name_label
import com.musicroom.app.generated.resources.playlist_photo_remove
import com.musicroom.app.generated.resources.playlist_settings
import com.musicroom.app.generated.resources.playlist_type
import com.musicroom.app.theme.components.AppBottomSheet
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.PlaylistValidation
import com.musicroom.shared.dto.PlaylistDetailResponse
import com.musicroom.shared.dto.PlaylistType
import com.musicroom.shared.dto.UpdatePlaylistRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Owner settings from the bottom: rename, switch personal / shared, remove the photo, delete the playlist. */
@Composable
fun PlaylistSettingsSheet(
    api: MusicRoomApi,
    detail: PlaylistDetailResponse,
    onChange: (PlaylistDetailResponse) -> Unit,
    onDeleted: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    val playlist = detail.playlist
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(playlist.name) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun run(action: suspend () -> Unit) {
        busy = true
        error = null
        scope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
            }
            busy = false
        }
    }

    AppBottomSheet(onDismiss) { close ->
        Text(stringResource(Res.string.playlist_settings), style = AppTheme.typography.title, color = colors.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            AppTextField(name, { name = it }, stringResource(Res.string.playlist_name_label), Modifier.weight(1f), enabled = !busy, leadingIcon = Lucide.ListMusic)
            AppButton(
                stringResource(Res.string.common_save),
                {
                    val invalid = PlaylistValidation.nameError(name)
                    if (invalid != null) error = UiText.of(invalid) else run { onChange(api.updatePlaylist(playlist.id, UpdatePlaylistRequest(name = name.trim()))) }
                },
                enabled = !busy && name.trim() != playlist.name,
            )
        }

        Text(stringResource(Res.string.playlist_type), style = AppTheme.typography.label, color = colors.textSecondary)
        PlaylistType.entries.forEach { type ->
            PlaylistTypeOption(
                type,
                selected = playlist.type == type,
                onClick = { if (playlist.type != type) run { onChange(api.updatePlaylist(playlist.id, UpdatePlaylistRequest(type = type))) } },
                enabled = !busy,
            )
        }

        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }

        if (playlist.coverVersion != null) {
            AppButton(
                stringResource(Res.string.playlist_photo_remove),
                { run { onChange(api.deletePlaylistCover(playlist.id)) } },
                Modifier.fillMaxWidth(),
                enabled = !busy,
                variant = AppButtonVariant.Secondary,
                icon = Lucide.ImageOff,
            )
        }
        AppButton(
            stringResource(Res.string.playlist_delete),
            { confirmDelete = true },
            Modifier.fillMaxWidth(),
            enabled = !busy,
            variant = AppButtonVariant.Ghost,
            icon = Lucide.Trash2,
        )

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                containerColor = colors.surface,
                shape = AppShapes.large,
                text = { Text(stringResource(Res.string.playlist_delete_confirm, playlist.name), style = AppTheme.typography.body, color = colors.textPrimary) },
                confirmButton = {
                    AppButton(stringResource(Res.string.playlist_delete), {
                        confirmDelete = false
                        run {
                            api.deletePlaylist(playlist.id)
                            close()
                            onDeleted()
                        }
                    })
                },
                dismissButton = {
                    AppButton(stringResource(Res.string.common_cancel), { confirmDelete = false }, variant = AppButtonVariant.Secondary)
                },
            )
        }
    }
}
