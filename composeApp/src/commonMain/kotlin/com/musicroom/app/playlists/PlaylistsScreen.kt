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
import com.musicroom.app.music.MusicPlaylist
import com.musicroom.app.music.ProviderSession
import com.musicroom.app.player.PlayerController
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PlaylistResponse
import com.musicroom.shared.dto.SocialCountsResponse
import com.musicroom.shared.dto.TrackSource
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.CancellationException

/**
 * The playlists made in Music Room (owned or shared with the user), then one shelf per connected provider
 * that lists the user's playlists. Opening one slides its page in place of the list. [openRoomPlaylistId] is
 * kept by the caller so a new playlist can open right after it is created.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlaylistsScreen(
    api: MusicRoomApi,
    user: UserResponse,
    player: PlayerController,
    onCreate: () -> Unit,
    openRoomPlaylistId: String?,
    openProviderPlaylistKey: String?,
    counts: SocialCountsResponse,
    onOpenNotifications: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenRoomPlaylist: (String?) -> Unit,
    onOpenProviderPlaylist: (String?) -> Unit,
) {
    val providers = ProviderSession.connections.current
    val playlistProviders = providers.filter { it.available && it.connected && it.playlistSource != null }
    var playlistsBySource by remember { mutableStateOf<Map<TrackSource, List<MusicPlaylist>>>(emptyMap()) }
    var errorsBySource by remember { mutableStateOf<Map<TrackSource, UiText>>(emptyMap()) }
    var roomPlaylists by remember { mutableStateOf<List<PlaylistResponse>?>(null) }
    var roomError by remember { mutableStateOf<UiText?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(playlistProviders.map { it.source }, attempt) {
        playlistsBySource = emptyMap()
        errorsBySource = emptyMap()
        playlistProviders.forEach { provider ->
            val source = provider.playlistSource ?: return@forEach
            try {
                playlistsBySource = playlistsBySource + (provider.source to source.playlists())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorsBySource = errorsBySource + (provider.source to UiText.of(e))
            }
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

    val shelves = playlistProviders.map { provider ->
        ProviderPlaylists(provider, playlistsBySource[provider.source], errorsBySource[provider.source])
    }

    // Which page shows: the list, or one playlist ("room:<id>" or "provider:<source>:<id>").
    val list = "list"
    val page = when {
        openRoomPlaylistId != null -> "room:$openRoomPlaylistId"
        openProviderPlaylistKey != null -> "provider:$openProviderPlaylistKey"
        else -> list
    }
    BackHandler(enabled = page != list) {
        onOpenRoomPlaylist(null)
        onOpenProviderPlaylist(null)
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
            target.startsWith("provider:") -> {
                val key = target.removePrefix("provider:")
                val playlist = shelves.firstNotNullOfOrNull { shelf -> shelf.playlists?.firstOrNull { it.key == key } }
                val provider = playlist?.let { found -> providers.firstOrNull { it.source == found.source } }
                if (playlist != null && provider != null) {
                    PlaylistDetail(
                        api, provider, playlist, player,
                        counts, onOpenNotifications, onOpenMessages,
                        onBack = { onOpenProviderPlaylist(null) },
                    )
                }
            }
            else -> PlaylistsList(
                api, roomPlaylists, roomError, shelves,
                onRetry = { attempt++ }, onCreate = onCreate,
                onOpenRoom = onOpenRoomPlaylist,
                onOpenProviderPlaylist = { onOpenProviderPlaylist(it.key) },
            )
        }
    }
}
