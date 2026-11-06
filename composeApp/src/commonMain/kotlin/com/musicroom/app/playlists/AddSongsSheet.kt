package com.musicroom.app.playlists

import androidx.compose.runtime.Composable
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.playlist_add_song
import com.musicroom.app.generated.resources.playlist_add_songs
import com.musicroom.app.generated.resources.playlist_song_added
import com.musicroom.app.music.TrackPickerSheet
import com.musicroom.app.player.PlayerController
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PlaylistDetailResponse
import org.jetbrains.compose.resources.stringResource

/**
 * Card from the bottom to add songs: the user's top tracks until they type, then Spotify search results.
 * The + of a song adds it at the end of the playlist and turns into a check mark.
 */
@Composable
fun AddSongsSheet(
    api: MusicRoomApi,
    detail: PlaylistDetailResponse,
    player: PlayerController,
    onChange: (PlaylistDetailResponse) -> Unit,
    onDismiss: () -> Unit,
) {
    val inPlaylist = detail.tracks.map { it.track.id }.toSet()
    TrackPickerSheet(
        api,
        player,
        title = stringResource(Res.string.playlist_add_songs),
        addLabel = stringResource(Res.string.playlist_add_song),
        addedLabel = stringResource(Res.string.playlist_song_added),
        isAdded = { it.id in inPlaylist },
        onAdd = { onChange(api.addPlaylistTrack(detail.playlist.id, it.id)) },
        onDismiss = onDismiss,
        allowAppleMusic = true,
    )
}
