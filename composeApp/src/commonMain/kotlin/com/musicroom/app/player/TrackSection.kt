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
import com.musicroom.app.generated.resources.player_open_app
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

@Composable
internal fun TrackSection(api: MusicRoomApi, player: PlayerController, onAddToPlaylist: () -> Unit) {
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
                // A song the app plays itself (uploaded or Apple Music) does not use a remote device.
                // The spacer keeps the row symmetric when the device button is hidden.
                if (player.canTransferPlayback) DeviceButton(player) else Spacer(Modifier.size(48.dp))
            }
        }

        val error = player.error
        if (error != null) {
            Text(error.resolve(), style = AppTheme.typography.bodySmall, color = colors.error)
            if (player.needsDevice) {
                player.remoteAppUrl?.let { url ->
                    AppButton(
                        stringResource(Res.string.player_open_app, player.remoteDisplayName),
                        { runCatching { uriHandler.openUri(url) } },
                        variant = AppButtonVariant.Secondary,
                    )
                }
            }
        }
    }
}

/** Solo queue: "Up next" (reorderable), then what Spotify plays on its own afterwards. */
