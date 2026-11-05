package com.musicroom.app.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.queue_add
import com.musicroom.app.generated.resources.queue_add_songs
import com.musicroom.app.generated.resources.queue_in_queue
import com.musicroom.app.music.TrackPickerSheet
import com.musicroom.shared.MusicRoomApi
import org.jetbrains.compose.resources.stringResource

/** Card from the bottom to find songs and add them at the end of the queue; added ones get a check mark. */
@Composable
fun AddToQueueSheet(api: MusicRoomApi, player: PlayerController, onDismiss: () -> Unit) {
    var queued by remember { mutableStateOf<Set<String>>(emptySet()) }
    TrackPickerSheet(
        api,
        player,
        title = stringResource(Res.string.queue_add_songs),
        addLabel = stringResource(Res.string.queue_add),
        addedLabel = stringResource(Res.string.queue_in_queue),
        isAdded = { it.id in queued },
        onAdd = { track ->
            player.addToQueue(listOf(track))
            queued = queued + track.id
        },
        onDismiss = onDismiss,
    )
}
