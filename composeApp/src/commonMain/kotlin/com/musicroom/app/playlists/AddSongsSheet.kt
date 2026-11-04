package com.musicroom.app.playlists

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.CirclePlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.musicroom.app.common.SectionTitle
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.playlist_add_song
import com.musicroom.app.generated.resources.playlist_add_songs
import com.musicroom.app.generated.resources.playlist_search_hint
import com.musicroom.app.generated.resources.playlist_song_added
import com.musicroom.app.generated.resources.reco_top_tracks
import com.musicroom.app.generated.resources.search_no_results
import com.musicroom.app.music.TrackRow
import com.musicroom.app.player.PlayerController
import com.musicroom.app.theme.components.AppBottomSheet
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PlaylistDetailResponse
import com.musicroom.shared.dto.TrackResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<TrackResponse>?>(null) }
    var adding by remember { mutableStateOf<Set<String>>(emptySet()) }
    var error by remember { mutableStateOf<UiText?>(null) }
    val inPlaylist = detail.tracks.map { it.track.id }.toSet()

    LaunchedEffect(query) {
        results = null
        val text = query.trim()
        if (text.isNotEmpty()) delay(350)
        results = try {
            if (text.isEmpty()) api.myTopTracks().items else api.searchTracks(text).items
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
            emptyList()
        }
    }

    fun add(track: TrackResponse) {
        if (track.id in inPlaylist || track.id in adding) return
        adding = adding + track.id
        error = null
        scope.launch {
            try {
                onChange(api.addPlaylistTrack(detail.playlist.id, track.id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
            }
            adding = adding - track.id
        }
    }

    AppBottomSheet(onDismiss) {
        Text(stringResource(Res.string.playlist_add_songs), style = AppTheme.typography.title, color = colors.textPrimary)
        AppTextField(query, { query = it }, stringResource(Res.string.playlist_search_hint), Modifier.fillMaxWidth(), leadingIcon = Lucide.Search)
        if (query.isBlank()) SectionTitle(stringResource(Res.string.reco_top_tracks))
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
        val loaded = results
        when {
            loaded == null -> Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            loaded.isEmpty() -> Text(stringResource(Res.string.search_no_results), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            else -> loaded.forEachIndexed { index, track ->
                val added = track.id in inPlaylist
                TrackRow(
                    api,
                    track,
                    playing = player.track?.id == track.id,
                    onClick = { player.play(loaded, index) },
                    trailing = {
                        IconButton(onClick = { add(track) }, enabled = !added && track.id !in adding) {
                            AnimatedContent(added, transitionSpec = { scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut() }) { done ->
                                Icon(
                                    if (done) Lucide.CircleCheck else Lucide.CirclePlus,
                                    contentDescription = stringResource(if (done) Res.string.playlist_song_added else Res.string.playlist_add_song),
                                    tint = if (done) colors.accentText else colors.textPrimary,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}
