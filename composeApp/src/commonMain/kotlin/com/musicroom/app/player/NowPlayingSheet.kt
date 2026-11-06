package com.musicroom.app.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CirclePlus
import com.composables.icons.lucide.ListPlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.common.SectionTitle
import com.musicroom.app.events.EventFocus
import com.musicroom.app.events.EventQueueList
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_close
import com.musicroom.app.generated.resources.now_playing_nothing
import com.musicroom.app.generated.resources.now_playing_title
import com.musicroom.app.generated.resources.queue_add_songs
import com.musicroom.app.generated.resources.queue_drag_hint
import com.musicroom.app.generated.resources.queue_empty
import com.musicroom.app.generated.resources.queue_then
import com.musicroom.app.generated.resources.queue_title
import com.musicroom.app.generated.resources.spotify_open
import com.musicroom.app.generated.resources.track_add_to_playlist
import com.musicroom.app.music.TrackMoreButton
import com.musicroom.app.music.TrackRow
import com.musicroom.app.playlists.AddToPlaylistSheet
import com.musicroom.app.theme.components.AppBottomSheet
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import org.jetbrains.compose.resources.stringResource

/**
 * Player sheet: large cover, title, seek bar and controls for the current track, a button to add it to a
 * playlist, then the queue ("Up next"). Songs are added to the queue from a card over it or by swiping a
 * song to the right; songs added in the app show who proposed them and can be moved (hold the handle, then
 * drag) or removed after tapping "Change the order". Tapping a queued song plays it.
 * While an event is focused, the queue is the event's: songs added to it are proposed to the event.
 */
@Composable
fun NowPlayingSheet(api: MusicRoomApi, player: PlayerController, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val uriHandler = LocalUriHandler.current
    var addingToQueue by remember { mutableStateOf(false) }
    var choosingPlaylist by remember { mutableStateOf(false) }
    var reordering by remember { mutableStateOf(false) }

    LaunchedEffect(player) { player.followQueue() }

    // The current track, its controls and the player messages.
    val trackSection: @Composable ColumnScope.() -> Unit = {
        val track = player.track
        if (track == null) {
            Text(stringResource(Res.string.now_playing_nothing), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
        } else {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                RemoteImage(
                    api,
                    track.imageUrl,
                    Modifier.widthIn(max = 280.dp).fillMaxWidth().aspectRatio(1f).clip(AppShapes.large),
                )
                Text(track.name, style = AppTheme.typography.heading, color = colors.textPrimary, textAlign = TextAlign.Center)
                Text(
                    listOf(track.artists.joinToString(", "), track.album).filter { it.isNotBlank() }.joinToString(" · "),
                    style = AppTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            SeekBar(player, Modifier.fillMaxWidth())
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkipButton(player, forward = false)
                PlayButton(player, 64.dp)
                SkipButton(player, forward = true)
            }
            // A song the app plays itself (uploaded or from Apple Music) does not play on a Spotify device.
            if (!player.playingHosted && !player.playingAppleMusic) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    DeviceButton(player)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                AppButton(
                    stringResource(Res.string.track_add_to_playlist),
                    { choosingPlaylist = true },
                    variant = AppButtonVariant.Secondary,
                    icon = Lucide.CirclePlus,
                )
            }
        }

        val error = player.error
        if (error != null) {
            Text(error.resolve(), style = AppTheme.typography.bodySmall, color = colors.error)
            if (player.needsDevice) {
                AppButton(
                    stringResource(Res.string.spotify_open),
                    { runCatching { uriHandler.openUri(PlayerController.SPOTIFY_WEB_URL) } },
                    variant = AppButtonVariant.Secondary,
                )
            }
        }
    }

    // "Up next": songs added in the app (with who proposed them; their order can change), then what
    // Spotify plays on its own after them (nothing shown next to those).
    val focus = EventFocus.Local.current
    val queueSection: @Composable ColumnScope.() -> Unit = queue@{
        // Listening to an event: its queue is the queue, and songs added to it are proposed to the event.
        if (focus?.event != null) {
            SectionTitle(stringResource(Res.string.queue_title))
            AppButton(
                stringResource(Res.string.queue_add_songs),
                { addingToQueue = true },
                variant = AppButtonVariant.Secondary,
                icon = Lucide.ListPlus,
            )
            EventQueueList(api, focus)
            return@queue
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(Res.string.queue_title), Modifier.weight(1f))
            AnimatedVisibility(
                player.upNext.size > 1 || reordering,
                enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = 400f)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                ReorderButton(reordering, onClick = { reordering = !reordering })
            }
        }
        AppButton(
            stringResource(Res.string.queue_add_songs),
            { addingToQueue = true },
            variant = AppButtonVariant.Secondary,
            icon = Lucide.ListPlus,
        )
        val upNext = player.upNext
        val following = player.following
        if (upNext.isEmpty() && following.isEmpty()) {
            Text(stringResource(Res.string.queue_empty), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
        }
        AnimatedVisibility(reordering, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Text(stringResource(Res.string.queue_drag_hint), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
        }
        UpNextList(api, player, reordering)
        if (upNext.isNotEmpty() && following.isNotEmpty()) {
            Text(stringResource(Res.string.queue_then), style = AppTheme.typography.label, color = colors.textSecondary)
        }
        Column {
            following.forEachIndexed { index, queued ->
                TrackRow(
                    api,
                    queued,
                    playing = false,
                    onClick = { player.playFollowing(index) },
                    trailing = { TrackMoreButton(api, player, queued) },
                    onSwipeRight = { player.addToQueue(listOf(queued)) },
                )
            }
        }
    }

    AppBottomSheet(onDismiss) { close ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.now_playing_title), Modifier.weight(1f), style = AppTheme.typography.heading, color = colors.textPrimary)
            IconButton(onClick = close) {
                Icon(Lucide.X, contentDescription = stringResource(Res.string.common_close), tint = colors.textSecondary)
            }
        }
        trackSection()
        queueSection()
    }

    if (addingToQueue) AddToQueueSheet(api, player, onDismiss = { addingToQueue = false })
    val current = player.track
    if (choosingPlaylist && current != null) AddToPlaylistSheet(api, current, onDismiss = { choosingPlaylist = false })
}
