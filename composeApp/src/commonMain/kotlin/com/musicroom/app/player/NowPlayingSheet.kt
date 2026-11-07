package com.musicroom.app.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CirclePlus
import com.composables.icons.lucide.ListPlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.common.SectionTitle
import com.musicroom.app.events.EventFocus
import com.musicroom.app.events.EventQueueList
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.event_nothing_playing
import com.musicroom.app.generated.resources.now_playing_nothing
import com.musicroom.app.generated.resources.queue_add_songs
import com.musicroom.app.generated.resources.queue_drag_hint
import com.musicroom.app.generated.resources.queue_empty
import com.musicroom.app.generated.resources.queue_propose_songs
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
 * Player sheet: it only accompanies playback, over a heavily blurred copy of the current cover. One contextual view:
 *  - cover (a grey music note while the focused event plays nothing), title, seek bar and controls (+ add to
 *    playlist / device as icons); while an event is focused, the controls drive its music and are greyed out for
 *    what the user cannot do;
 *  - the queue: the user's own ("Up next", reorderable) or the event's;
 *  - a pinned action bar: add songs (solo) or propose songs (event).
 * Playing a song of their own makes the user leave the event. Discovering or browsing events lives outside this sheet.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NowPlayingSheet(
    api: MusicRoomApi,
    player: PlayerController,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    val focus = EventFocus.Local.current
    val event = focus?.event
    var addingToQueue by remember { mutableStateOf(false) }
    var choosingPlaylist by remember { mutableStateOf(false) }

    LaunchedEffect(player) { player.followQueue() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxHeight(),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        containerColor = colors.background,
        contentColor = colors.textPrimary,
        // The backdrop goes up to the top edge: the handle is drawn below, over it.
        dragHandle = null,
    ) {
        Box(Modifier.fillMaxSize()) {
            CoverBackdrop(api, player.track?.imageUrl, Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                        .padding(start = AppTheme.spacing.lg, end = AppTheme.spacing.lg, top = AppTheme.spacing.xl, bottom = AppTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                ) {
                    TrackSection(api, player, onAddToPlaylist = { choosingPlaylist = true })
                    if (event != null && focus != null) {
                        SectionTitle(stringResource(Res.string.queue_title), color = Color.White)
                        EventQueueList(api, focus)
                    } else {
                        SoloQueue(api, player)
                    }
                }
                // Pinned primary action: always reachable, whatever the queue length.
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding()
                        .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.sm),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AppButton(
                        stringResource(if (event != null) Res.string.queue_propose_songs else Res.string.queue_add_songs),
                        { addingToQueue = true },
                        enabled = event?.canVote ?: true,
                        icon = Lucide.ListPlus,
                    )
                }
            }
            Box(
                Modifier.align(Alignment.TopCenter).padding(top = AppTheme.spacing.lg)
                    .size(width = 36.dp, height = 4.dp)
                    .background(colors.textSecondary.copy(alpha = 0.5f), CircleShape),
            )
        }
    }

    if (addingToQueue) {
        if (focus?.event != null) {
            ProposeToEventSheet(api, player, focus, onDismiss = { addingToQueue = false })
        } else {
            AddToQueueSheet(api, player, onDismiss = { addingToQueue = false })
        }
    }
    val current = player.track
    if (choosingPlaylist && current != null) AddToPlaylistSheet(api, current, onDismiss = { choosingPlaylist = false })
}

/**
 * The current cover, enlarged and blurred out of recognition, under a veil of the background color so the text
 * stays readable. It fades to the next one when the song changes. (Android before 12 ignores the blur.)
 */
@Composable
private fun CoverBackdrop(api: MusicRoomApi, imageUrl: String?, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Box(modifier.clipToBounds()) {
        Crossfade(imageUrl, Modifier.fillMaxSize(), label = "nowPlayingBackdrop") { url ->
            if (url != null) {
                RemoteImage(
                    api,
                    url,
                    Modifier.fillMaxSize().graphicsLayer {
                        scaleX = 1.5f
                        scaleY = 1.5f
                    }.blur(80.dp),
                )
            }
        }
    }
}

/**
 * Cover, title, seek bar and one symmetric controls row:
 * [playlist] [prev] [play] [next] [device]. While an event is
 * focused, the buttons drive its music: they show even before anything plays (so its owner can start it) and are
 * greyed out for what the user cannot do (see [PlayerController.canTogglePlay]); an event playing nothing shows an
 * empty cover with a grey music note.
 */
@Composable
private fun TrackSection(api: MusicRoomApi, player: PlayerController, onAddToPlaylist: () -> Unit) {
    val colors = AppTheme.colors
    val uriHandler = LocalUriHandler.current
    val track = player.track
    val inEvent = player.eventRemote != null

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        Spacer(Modifier.height(80.dp))
        if (track == null) {
            if (inEvent) {
                Box(
                    Modifier.widthIn(max = 280.dp).fillMaxWidth().aspectRatio(1f).clip(AppShapes.large).background(colors.surfaceSubtle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Lucide.Music, contentDescription = null, tint = Color.White, modifier = Modifier.size(88.dp))
                }
            }
            Text(
                stringResource(if (inEvent) Res.string.event_nothing_playing else Res.string.now_playing_nothing),
                style = AppTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        } else {
            RemoteImage(
                api,
                track.imageUrl,
                Modifier.widthIn(max = 280.dp).fillMaxWidth().aspectRatio(1f).clip(AppShapes.large),
            )
            Text(track.name, style = AppTheme.typography.heading, color = Color.White, textAlign = TextAlign.Center)
            Text(
                listOf(track.artists.joinToString(", "), track.album).filter { it.isNotBlank() }.joinToString(" · "),
                style = AppTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
        if (track != null || inEvent) {
            SeekBar(player, Modifier.fillMaxWidth())
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onAddToPlaylist, enabled = track != null) {
                    Icon(
                        imageVector = Lucide.CirclePlus,
                        contentDescription = stringResource(Res.string.track_add_to_playlist),
                        tint = Color.White,
                    )
                }
                SkipButton(player, forward = false)
                PlayButton(player, 64.dp)
                SkipButton(player, forward = true)
                // A song the app plays itself (uploaded or Apple Music) does not use a Spotify device.
                // The spacer keeps the row symmetric when the device button is hidden.
                if (!player.playingHosted && !player.playingAppleMusic) DeviceButton(player) else Spacer(Modifier.size(48.dp))
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
}

/** Solo queue: "Up next" (reorderable), then what Spotify plays on its own afterwards. */
@Composable
private fun SoloQueue(api: MusicRoomApi, player: PlayerController) {
    var reordering by remember { mutableStateOf(false) }
    val upNext = player.upNext
    val following = player.following

    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(Res.string.queue_title), Modifier.weight(1f), color = Color.White)
            AnimatedVisibility(
                upNext.size > 1 || reordering,
                enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = 400f)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                ReorderButton(reordering, onClick = { reordering = !reordering })
            }
        }
        if (upNext.isEmpty() && following.isEmpty()) {
            Text(stringResource(Res.string.queue_empty), style = AppTheme.typography.bodySmall, color = Color.White)
        }
        AnimatedVisibility(reordering, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Text(stringResource(Res.string.queue_drag_hint), style = AppTheme.typography.bodySmall, color = Color.White)
        }
        UpNextList(api, player, reordering)
        if (upNext.isNotEmpty() && following.isNotEmpty()) {
            Text(stringResource(Res.string.queue_then), style = AppTheme.typography.label, color = Color.White)
        }
        Column {
            following.forEachIndexed { index, queued ->
                TrackRow(
                    api,
                    queued,
                    playing = false,
                    onClick = { player.playFollowing(index) },
                    trailing = { TrackMoreButton(api, player, queued, tint = Color.White) },
                    onSwipeRight = { player.addToQueue(listOf(queued)) },
                    upNextList = true
                )
            }
        }
    }
}