package com.musicroom.app.events

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_cancel
import com.musicroom.app.generated.resources.common_save
import com.musicroom.app.generated.resources.event_delete
import com.musicroom.app.generated.resources.event_delete_confirm
import com.musicroom.app.generated.resources.event_settings
import com.musicroom.app.theme.components.AppBottomSheet
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.CreateEventRequest
import com.musicroom.shared.dto.EventResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** The owner changes the event's settings, or deletes it (after a confirmation; then [onDeleted]). */
@Composable
fun EventSettingsSheet(api: MusicRoomApi, room: EventRoom, event: EventResponse, onDismiss: () -> Unit, onDeleted: () -> Unit) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }
    val initial = CreateEventRequest(
        name = event.name,
        description = event.description,
        visibility = event.visibility,
        voteLicense = event.voteLicense,
        location = event.location,
        radiusMeters = event.radiusMeters,
        startsAt = event.startsAt,
        endsAt = event.endsAt,
    )

    AppBottomSheet(onDismiss) { close ->
        Text(stringResource(Res.string.event_settings), style = AppTheme.typography.title, color = colors.textPrimary)
        EventForm(initial, stringResource(Res.string.common_save)) { request ->
            room.replace(api.updateEvent(event.id, request))
            close()
        }
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
        AppButton(
            stringResource(Res.string.event_delete),
            { confirmDelete = true },
            Modifier.fillMaxWidth(),
            variant = AppButtonVariant.Ghost,
            icon = Lucide.Trash2,
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = colors.surface,
            shape = AppShapes.large,
            text = { Text(stringResource(Res.string.event_delete_confirm, event.name), style = AppTheme.typography.body, color = colors.textPrimary) },
            confirmButton = {
                AppButton(stringResource(Res.string.event_delete), {
                    confirmDelete = false
                    scope.launch {
                        try {
                            api.deleteEvent(event.id)
                            onDeleted()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = UiText.of(e)
                        }
                    }
                })
            },
            dismissButton = { AppButton(stringResource(Res.string.common_cancel), { confirmDelete = false }, variant = AppButtonVariant.Ghost) },
        )
    }
}
