package com.musicroom.app.create

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.SlidersHorizontal
import com.composables.icons.lucide.Vote
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.create_control_body
import com.musicroom.app.generated.resources.create_control_title
import com.musicroom.app.generated.resources.create_event_body
import com.musicroom.app.generated.resources.create_event_title
import com.musicroom.app.generated.resources.create_playlist_body
import com.musicroom.app.generated.resources.create_playlist_title
import com.musicroom.app.generated.resources.create_title
import com.musicroom.app.theme.components.AppBottomSheet
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PlaylistResponse
import org.jetbrains.compose.resources.stringResource

/**
 * The Create button: a card sliding up from the bottom with the three services of the project. Picking
 * "Playlist" or "Event" slides to its form inside the same card.
 */
@Composable
fun CreateSheet(
    api: MusicRoomApi,
    onDismiss: () -> Unit,
    onPlaylistCreated: (PlaylistResponse) -> Unit,
    onEventCreated: (String) -> Unit,
) {
    // The form shown: "playlist", "event", or the menu (empty).
    var form by remember { mutableStateOf("") }
    AppBottomSheet(onDismiss) { _ ->
        AnimatedContent(
            form,
            transitionSpec = {
                val forward = targetState.isNotEmpty()
                (slideInHorizontally(tween(260)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(260)))
                    .togetherWith(slideOutHorizontally(tween(200)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(150)))
            },
        ) { shown ->
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                if (shown == "playlist") {
                    NewPlaylistForm(api, onBack = { form = "" }, onCreated = onPlaylistCreated)
                } else if (shown == "event") {
                    NewEventForm(api, onBack = { form = "" }, onCreated = onEventCreated)
                } else {
                    Text(stringResource(Res.string.create_title), style = AppTheme.typography.title, color = AppTheme.colors.textPrimary)
                    CreateOptionCard(
                        Lucide.ListMusic,
                        stringResource(Res.string.create_playlist_title),
                        stringResource(Res.string.create_playlist_body),
                        onClick = { form = "playlist" },
                    )
                    CreateOptionCard(
                        Lucide.Vote,
                        stringResource(Res.string.create_event_title),
                        stringResource(Res.string.create_event_body),
                        onClick = { form = "event" },
                    )
                    CreateOptionCard(
                        Lucide.SlidersHorizontal,
                        stringResource(Res.string.create_control_title),
                        stringResource(Res.string.create_control_body),
                    )
                }
            }
        }
    }
}
