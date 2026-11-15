package com.musicroom.app.playlists

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.CirclePlus
import com.composables.icons.lucide.Lucide
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.add_to_playlist_done
import com.musicroom.app.generated.resources.add_to_playlist_empty
import com.musicroom.app.generated.resources.add_to_playlist_read_only
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.playlist_add_song
import com.musicroom.app.generated.resources.playlist_song_added
import com.musicroom.app.generated.resources.track_add_to_playlist
import com.musicroom.app.theme.components.AppBottomSheet
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PlaylistResponse
import com.musicroom.shared.dto.TrackResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Card from the bottom listing all the user's Music Room playlists, to add [track] to one of them.
 * Playlists the user can only listen to are dimmed. The card closes shortly after the song is added.
 */
@Composable
fun AddToPlaylistSheet(api: MusicRoomApi, track: TrackResponse, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var playlists by remember { mutableStateOf<List<PlaylistResponse>?>(null) }
    var adding by remember { mutableStateOf<String?>(null) }
    var added by remember { mutableStateOf<PlaylistResponse?>(null) }
    var error by remember { mutableStateOf<UiText?>(null) }

    LaunchedEffect(Unit) {
        playlists = try {
            api.roomPlaylists().items
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
            emptyList()
        }
    }

    AppBottomSheet(onDismiss) { close ->
        fun add(playlist: PlaylistResponse) {
            if (adding != null || added != null) return
            adding = playlist.id
            error = null
            scope.launch {
                try {
                    api.addPlaylistTrack(playlist.id, track)
                    added = playlist
                    delay(700)
                    close()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error = UiText.of(e)
                }
                adding = null
            }
        }

        Text(stringResource(Res.string.track_add_to_playlist), style = AppTheme.typography.title, color = colors.textPrimary)
        Text(
            added?.let { stringResource(Res.string.add_to_playlist_done, it.name) } ?: track.name,
            style = AppTheme.typography.bodySmall,
            color = if (added != null) colors.accentText else colors.textSecondary,
        )
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
        val loaded = playlists
        when {
            loaded == null -> Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            loaded.isEmpty() && error == null ->
                Text(stringResource(Res.string.add_to_playlist_empty), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            else -> loaded.forEach { playlist ->
                val done = added?.id == playlist.id
                Row(
                    Modifier.fillMaxWidth().alpha(if (playlist.canEdit) 1f else 0.5f),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoomPlaylistRow(api, playlist, onClick = { if (playlist.canEdit) add(playlist) }, modifier = Modifier.weight(1f))
                    if (playlist.canEdit) {
                        AnimatedContent(done, transitionSpec = { scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut() }) { check ->
                            Icon(
                                if (check) Lucide.CircleCheck else Lucide.CirclePlus,
                                contentDescription = stringResource(if (check) Res.string.playlist_song_added else Res.string.playlist_add_song),
                                tint = if (check) colors.accentText else colors.textSecondary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    } else {
                        Text(stringResource(Res.string.add_to_playlist_read_only), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}
