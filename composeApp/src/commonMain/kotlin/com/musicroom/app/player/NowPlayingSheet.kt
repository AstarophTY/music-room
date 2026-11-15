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
import com.musicroom.app.music.TrackMoreButton
import com.musicroom.app.music.TrackRow
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
                    TrackSection(api, player)
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
}

/**
 * The current cover, enlarged and blurred out of recognition, under a veil of the background color so the text
 * stays readable. It fades to the next one when the song changes. (Android before 12 ignores the blur.)
 */
