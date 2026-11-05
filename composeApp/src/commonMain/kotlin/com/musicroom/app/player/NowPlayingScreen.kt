package com.musicroom.app.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.CirclePlus
import com.composables.icons.lucide.ListPlus
import com.composables.icons.lucide.Lucide
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.common.SectionTitle
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.common.WindowSize
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_back
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
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import org.jetbrains.compose.resources.stringResource

/**
 * Player page: large cover, title, seek bar and controls for the current track, a button to add it to a
 * playlist, then the queue ("Up next"). Phones show them one under the other; large screens put the track
 * on the left and the queue on the right, each scrolling on its own. Songs are added to the queue from a
 * card at the bottom or by swiping a song to the right; songs added in the app show who proposed them and
 * can be moved (hold the handle, then drag) or removed after tapping "Change the order". Tapping a queued song plays it.
 */
@Composable
fun NowPlayingScreen(api: MusicRoomApi, player: PlayerController, onBack: () -> Unit) {
    val colors = AppTheme.colors
    val uriHandler = LocalUriHandler.current
    val wide = WindowSize.current == WindowSize.Expanded
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
                    Modifier.widthIn(max = if (wide) 400.dp else 320.dp).fillMaxWidth().aspectRatio(1f).clip(AppShapes.large),
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                DeviceButton(player)
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
    val queueSection: @Composable ColumnScope.() -> Unit = {
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

    if (wide) {
        Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(AppTheme.spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Lucide.ArrowLeft, contentDescription = stringResource(Res.string.common_back), tint = colors.textPrimary)
                }
                Text(stringResource(Res.string.now_playing_title), style = AppTheme.typography.heading, color = colors.textPrimary)
            }
            Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = AppTheme.spacing.lg)) {
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
                        .padding(vertical = AppTheme.spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                ) {
                    Column(
                        Modifier.widthIn(max = 480.dp),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                        content = trackSection,
                    )
                }
                Box(
                    Modifier.padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.md)
                        .width(AppTheme.spacing.border).fillMaxHeight().background(colors.border),
                )
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
                        .padding(vertical = AppTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                    content = queueSection,
                )
            }
        }
    } else {
        SubScreenScaffold(title = stringResource(Res.string.now_playing_title), onBack = onBack) {
            trackSection()
            queueSection()
        }
    }

    if (addingToQueue) AddToQueueSheet(api, player, onDismiss = { addingToQueue = false })
    val current = player.track
    if (choosingPlaylist && current != null) AddToPlaylistSheet(api, current, onDismiss = { choosingPlaylist = false })
}
