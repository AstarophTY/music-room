package com.musicroom.app.create

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.musicroom.app.events.EventForm
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_back
import com.musicroom.app.generated.resources.event_create_button
import com.musicroom.app.generated.resources.event_new
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import org.jetbrains.compose.resources.stringResource

/** A new event, from the create card. [onCreated] gets its id. */
@Composable
fun NewEventForm(api: MusicRoomApi, onBack: () -> Unit, onCreated: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Lucide.ArrowLeft, contentDescription = stringResource(Res.string.common_back), tint = AppTheme.colors.textPrimary)
        }
        Text(stringResource(Res.string.event_new), style = AppTheme.typography.title, color = AppTheme.colors.textPrimary)
    }
    EventForm(initial = null, submitLabel = stringResource(Res.string.event_create_button)) { request ->
        onCreated(api.createEvent(request).event.id)
    }
}
