package com.musicroom.app.music

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.CirclePlus
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.ListPlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.playlist_remove_song
import com.musicroom.app.generated.resources.queue_add
import com.musicroom.app.generated.resources.track_add_to_playlist
import com.musicroom.app.generated.resources.track_more
import com.musicroom.app.player.PlayerController
import com.musicroom.app.player.Proposer
import com.musicroom.app.playlists.AddToPlaylistSheet
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TrackResponse
import org.jetbrains.compose.resources.stringResource

/**
 * The "..." of a track: a menu to add it to the queue (as proposed by [proposer], the user by default) or to
 * one of the user's playlists, and, when [onRemove] is given (an editor in a playlist), to remove it from the
 * playlist.
 */
@Composable
fun TrackMoreButton(
    api: MusicRoomApi,
    player: PlayerController,
    track: TrackResponse,
    proposer: Proposer? = null,
    onRemove: (() -> Unit)? = null,
    tint: Color? = null,
) {
    val colors = AppTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    var choosingPlaylist by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(Lucide.Ellipsis, contentDescription = stringResource(Res.string.track_more), tint = tint ?: colors.textSecondary)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = colors.surface) {
            @Composable
            fun item(label: String, icon: ImageVector, onClick: () -> Unit) = DropdownMenuItem(
                text = { Text(label, style = AppTheme.typography.label, color = colors.textPrimary) },
                leadingIcon = { Icon(icon, contentDescription = null, tint = colors.textPrimary) },
                onClick = {
                    menuOpen = false
                    onClick()
                },
            )
            item(stringResource(Res.string.queue_add), Lucide.ListPlus) { player.addToQueue(listOf(track), proposer) }
            item(stringResource(Res.string.track_add_to_playlist), Lucide.CirclePlus) { choosingPlaylist = true }
            if (onRemove != null) item(stringResource(Res.string.playlist_remove_song), Lucide.Trash2, onRemove)
        }
    }
    if (choosingPlaylist) AddToPlaylistSheet(api, track, onDismiss = { choosingPlaylist = false })
}
