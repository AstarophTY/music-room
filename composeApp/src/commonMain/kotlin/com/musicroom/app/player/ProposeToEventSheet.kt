package com.musicroom.app.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.musicroom.app.events.EventFocus
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.event_propose
import com.musicroom.app.generated.resources.event_propose_added
import com.musicroom.app.generated.resources.event_propose_title
import com.musicroom.app.music.TrackPickerSheet
import com.musicroom.shared.MusicRoomApi
import org.jetbrains.compose.resources.stringResource

/**
 * Sheet from the bottom to find songs and propose them to the focused event. Songs already in its queue (or
 * playing next) show a check mark. Apple Music is allowed: proposing only sends the song to the event, so the
 * app does not need to play it here.
 */
@Composable
fun ProposeToEventSheet(api: MusicRoomApi, player: PlayerController, focus: EventFocus, onDismiss: () -> Unit) {
    val detail = focus.detail
    val pendingIds = remember(detail) {
        (detail?.queue.orEmpty() + listOfNotNull(detail?.next, detail?.nowPlaying)).map { it.track.id }.toSet()
    }
    TrackPickerSheet(
        api,
        player,
        title = stringResource(Res.string.event_propose_title),
        addLabel = stringResource(Res.string.event_propose),
        addedLabel = stringResource(Res.string.event_propose_added),
        isAdded = { it.id in pendingIds },
        onAdd = { focus.propose(it) },
        onDismiss = onDismiss,
        showTopTracks = player.spotifyConnected,
        allowAppleMusic = true,
    )
}
