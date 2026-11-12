package com.musicroom.app.create

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.musicroom.app.events.EventForm
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.event_create_button
import com.musicroom.app.generated.resources.event_new
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import org.jetbrains.compose.resources.stringResource

/** A new event, started from the Events tab. [onCreated] gets its id. */
@Composable
fun NewEventForm(api: MusicRoomApi, onCreated: (String) -> Unit) {
    Text(stringResource(Res.string.event_new), style = AppTheme.typography.title, color = AppTheme.colors.textPrimary)
    EventForm(initial = null, submitLabel = stringResource(Res.string.event_create_button)) { request ->
        onCreated(api.createEvent(request).event.id)
    }
}
