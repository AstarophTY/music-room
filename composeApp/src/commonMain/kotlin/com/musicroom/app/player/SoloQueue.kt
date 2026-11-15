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
internal fun SoloQueue(api: MusicRoomApi, player: PlayerController) {
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