package com.musicroom.app.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import com.composables.icons.lucide.Play
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.messages_title
import com.musicroom.app.generated.resources.notifications_title
import com.musicroom.app.generated.resources.playlists_by
import com.musicroom.app.generated.resources.playlists_open_provider
import com.musicroom.app.generated.resources.playlists_play
import com.musicroom.app.generated.resources.playlists_track_count
import com.musicroom.app.generated.resources.search_load_more
import com.musicroom.app.music.MusicPlaylist
import com.musicroom.app.music.MusicProviderConnection
import com.musicroom.app.music.TrackMoreButton
import com.musicroom.app.music.TrackRow
import com.musicroom.app.player.PlayerController
import com.musicroom.app.social.BadgedIconButton
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.SocialCountsResponse
import com.musicroom.shared.dto.TrackResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A playlist of a connected provider: cover, Play and Open in the provider, then its tracks page by page.
 * Tapping a track plays the playlist from it. Some providers hide the tracks of playlists the user doesn't
 * own; those can still be played. The listed tracks can be added to the queue together, or one by one by
 * swiping them to the right or from their "..." (also to a playlist).
 */
@Composable
fun PlaylistDetail(
    api: MusicRoomApi,
    provider: MusicProviderConnection,
    playlist: MusicPlaylist,
    player: PlayerController,
    counts: SocialCountsResponse,
    onOpenNotifications: () -> Unit,
    onOpenMessages: () -> Unit,
    onBack: () -> Unit,
) {
    val playlistSource = requireNotNull(provider.playlistSource)
    val colors = AppTheme.colors
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var tracks by remember(playlist.id) { mutableStateOf<List<TrackResponse>>(emptyList()) }
    var nextOffset by remember(playlist.id) { mutableStateOf<Int?>(null) }
    var loading by remember(playlist.id) { mutableStateOf(true) }
    var error by remember(playlist.id) { mutableStateOf<UiText?>(null) }

    suspend fun load(offset: Int) {
        loading = true
        error = null
        try {
            val page = playlistSource.tracks(playlist, offset)
            tracks = (if (offset == 0) page.items else tracks + page.items).distinctBy { it.id }
            nextOffset = page.nextOffset
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
        }
        loading = false
    }

    LaunchedEffect(playlist.id) { load(0) }

    SubScreenScaffold(
        title = playlist.name,
        onBack = onBack,
        actions = {
            BadgedIconButton(Lucide.Bell, stringResource(Res.string.notifications_title), counts.unreadNotifications, onOpenNotifications)
            BadgedIconButton(Lucide.MessageCircle, stringResource(Res.string.messages_title), counts.unreadMessages, onOpenMessages)
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteImage(api, playlist.imageUrl, Modifier.size(120.dp).clip(AppShapes.large))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                playlist.owner?.let {
                    Text(stringResource(Res.string.playlists_by, it), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                }
                playlist.trackCount?.let {
                    Text(
                        pluralStringResource(Res.plurals.playlists_track_count, it, it),
                        style = AppTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            AppButton(
                stringResource(Res.string.playlists_play),
                { player.playPlaylist(playlist, tracks, 0) },
                enabled = playlist.contextUri != null || tracks.isNotEmpty(),
                icon = Lucide.Play,
            )
            playlist.externalUrl?.let { url ->
                AppButton(
                    stringResource(Res.string.playlists_open_provider, provider.displayName),
                    { runCatching { uriHandler.openUri(url) } },
                    variant = AppButtonVariant.Secondary,
                    icon = Lucide.ExternalLink,
                )
            }
        }

        val failure = error
        when {
            tracks.isEmpty() && loading ->
                Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            else -> {
                tracks.forEachIndexed { index, track ->
                    TrackRow(
                        api,
                        track,
                        playing = player.track?.id == track.id,
                        onClick = { player.playPlaylist(playlist, tracks, index) },
                        trailing = { TrackMoreButton(api, player, track) },
                        onSwipeRight = { player.addToQueue(listOf(track)) },
                    )
                }
                failure?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
                val offset = nextOffset
                if (offset != null) {
                    AppButton(
                        stringResource(Res.string.search_load_more),
                        { scope.launch { load(offset) } },
                        Modifier.fillMaxWidth().padding(top = AppTheme.spacing.sm),
                        enabled = !loading,
                        variant = AppButtonVariant.Secondary,
                    )
                }
            }
        }
    }
}
