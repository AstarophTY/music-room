package com.musicroom.app.playlists

import androidx.compose.runtime.Composable
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.playlist_add_song
import com.musicroom.app.generated.resources.playlist_add_songs
import com.musicroom.app.generated.resources.playlist_song_added
import com.musicroom.app.music.TrackPickerPage
import com.musicroom.app.player.PlayerController
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PlaylistDetailResponse
import org.jetbrains.compose.resources.stringResource

/**
 * Page for adding songs to a playlist.
 * The + of a song adds it at the end of the playlist and turns into a check mark.
 */
@Composable
fun AddSongsPage(
    api: MusicRoomApi,
    detail: PlaylistDetailResponse,
    player: PlayerController,
    onChange: (PlaylistDetailResponse) -> Unit,
    onBack: () -> Unit,
) {
    val inPlaylist = detail.tracks.map { it.track.id }.toSet()
    TrackPickerPage(
        api,
        player,
        title = stringResource(Res.string.playlist_add_songs),
        addLabel = stringResource(Res.string.playlist_add_song),
        addedLabel = stringResource(Res.string.playlist_song_added),
        isAdded = { it.id in inPlaylist },
        onAdd = { onChange(api.addPlaylistTrack(detail.playlist.id, it.id)) },
        onBack = onBack,
        allowAppleMusic = true,
    )
}
