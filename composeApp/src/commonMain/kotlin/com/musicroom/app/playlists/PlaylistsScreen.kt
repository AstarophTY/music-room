package com.musicroom.app.playlists

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import com.musicroom.app.common.UiText
import com.musicroom.app.player.PlayerController
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PlaylistResponse
import com.musicroom.shared.dto.PlaylistSummaryResponse
import com.musicroom.shared.dto.SocialCountsResponse
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.CancellationException

/**
 * The playlists made in Music Room (owned or shared with the user), then the ones the user created on their
 * Spotify account. Opening one slides its page in place of the list. [openRoomPlaylistId] is kept by the
 * caller so a new playlist can open right after it is created.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlaylistsScreen(
    api: MusicRoomApi,
    user: UserResponse,
    player: PlayerController,
    onCreate: () -> Unit,
    openRoomPlaylistId: String?,
    openSpotifyPlaylistId: String?,
    counts: SocialCountsResponse,
    onOpenNotifications: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenRoomPlaylist: (String?) -> Unit,
    onOpenSpotifyPlaylist: (String?) -> Unit,
) {
    var spotifyPlaylists by remember { mutableStateOf<List<PlaylistSummaryResponse>?>(null) }
    var roomPlaylists by remember { mutableStateOf<List<PlaylistResponse>?>(null) }
    var spotifyError by remember { mutableStateOf<UiText?>(null) }
    var roomError by remember { mutableStateOf<UiText?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(user.spotifyConnected, attempt) {
        if (!user.spotifyConnected) return@LaunchedEffect
        spotifyError = null
        try {
            spotifyPlaylists = api.myPlaylists().items
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            spotifyError = UiText.of(e)
        }
    }
    // Reloaded each time the list shows again, so changes made in a playlist appear.
    LaunchedEffect(openRoomPlaylistId == null, attempt) {
        if (openRoomPlaylistId != null) return@LaunchedEffect
        roomError = null
        try {
            roomPlaylists = api.roomPlaylists().items
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            roomError = UiText.of(e)
        }
    }

    // Which page shows: the list, or one playlist ("room:<id>" or "spotify:<id>").
    val list = "list"
    val page = when {
        openRoomPlaylistId != null -> "room:$openRoomPlaylistId"
        openSpotifyPlaylistId != null -> "spotify:$openSpotifyPlaylistId"
        else -> list
    }
    BackHandler(enabled = page != list) {
        onOpenRoomPlaylist(null)
        onOpenSpotifyPlaylist(null)
    }

    AnimatedContent(
        page,
        transitionSpec = {
            val opening = targetState != list
            (slideInHorizontally(tween(280)) { if (opening) it / 2 else -it / 4 } + fadeIn(tween(280)))
                .togetherWith(slideOutHorizontally(tween(220)) { if (opening) -it / 4 else it / 2 } + fadeOut(tween(180)))
        },
    ) { target ->
        when {
            target.startsWith("room:") ->
                RoomPlaylistScreen(
                    api, target.removePrefix("room:"), user.id, player,
                    counts, onOpenNotifications, onOpenMessages,
                    onBack = { onOpenRoomPlaylist(null) },
                )
            target.startsWith("spotify:") -> {
                val playlist = spotifyPlaylists?.firstOrNull { it.id == target.removePrefix("spotify:") }
                if (playlist != null) {
                    PlaylistDetail(
                        api, playlist, player,
                        counts, onOpenNotifications, onOpenMessages,
                        onBack = { onOpenSpotifyPlaylist(null) },
                    )
                }
            }
            else -> PlaylistsList(
                api, user, roomPlaylists, roomError, spotifyPlaylists, spotifyError,
                onRetry = { attempt++ }, onCreate = onCreate,
                onOpenRoom = onOpenRoomPlaylist, onOpenSpotify = { onOpenSpotifyPlaylist(it) },
            )
        }
    }
}
