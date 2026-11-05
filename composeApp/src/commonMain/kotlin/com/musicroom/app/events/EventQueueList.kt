package com.musicroom.app.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    val colors = AppTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        Text(stringResource(Res.string.event_focus_queue_hint, event.name), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
        detail?.next?.let { next ->
            Text(stringResource(Res.string.event_next), style = AppTheme.typography.label, color = colors.textSecondary)
            EventTrackRow(api, next, rank = null)
        }
        if (detail != null && detail.queue.isEmpty()) {
            Text(stringResource(Res.string.event_queue_empty_body), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Column {
            detail?.queue?.forEachIndexed { index, entry ->
                EventTrackRow(api, entry, rank = index + 1) {
                    VoteButton(entry.votes, entry.voted, enabled = event.canVote, onClick = { focus.toggleVote(entry.id) })
                }
            }
        }
    }
}
