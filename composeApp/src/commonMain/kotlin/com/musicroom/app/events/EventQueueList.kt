package com.musicroom.app.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.event_focus_queue_hint
import com.musicroom.app.generated.resources.event_next
import com.musicroom.app.generated.resources.event_queue_empty_body
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import org.jetbrains.compose.resources.stringResource

/**
 * The player's queue while an event is focused: the event's queue, where songs added to the queue are proposed.
 * The song chosen to play next comes first, then the others, most voted first, each with its vote button.
 */
@Composable
fun EventQueueList(api: MusicRoomApi, focus: EventFocus, modifier: Modifier = Modifier) {
    val event = focus.event ?: return
    val detail = focus.detail
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        Text(stringResource(Res.string.event_focus_queue_hint, event.name), style = AppTheme.typography.bodySmall, color = Color.White)
        detail?.next?.let { next ->
            Text(stringResource(Res.string.event_next), style = AppTheme.typography.label, color = Color.White)
            EventTrackRow(api, next, rank = null, lightContent = true)
        }
        if (detail != null && detail.queue.isEmpty()) {
            Text(stringResource(Res.string.event_queue_empty_body), style = AppTheme.typography.bodySmall, color = Color.White)
        }
        Column {
            detail?.queue?.forEachIndexed { index, entry ->
                EventTrackRow(api, entry, rank = index + 1, lightContent = true) {
                    VoteButton(entry.votes, entry.voted, enabled = event.canVote, onClick = { focus.toggleVote(entry.id) })
                }
            }
        }
    }
}
