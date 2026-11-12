package com.musicroom.app.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.musicroom.app.create.NewEventForm
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.audio_providers_home_action
import com.musicroom.app.generated.resources.event_host_provider_required
import com.musicroom.app.music.MusicProviderConnection
import com.musicroom.app.theme.components.AppBottomSheet
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import org.jetbrains.compose.resources.stringResource

/**
 * Starts an event, from the Events tab only. An event plays its music on a provider of its host, so it
 * asks for one to be connected first rather than letting a room be created with nothing to play on.
 */
@Composable
fun NewEventSheet(
    api: MusicRoomApi,
    providers: List<MusicProviderConnection>,
    onOpenProviders: () -> Unit,
    onDismiss: () -> Unit,
    onCreated: (String) -> Unit,
) {
    AppBottomSheet(onDismiss) { _ ->
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            if (providers.any { it.available && it.connected }) {
                NewEventForm(api, onCreated = onCreated)
            } else {
                Text(
                    stringResource(Res.string.event_host_provider_required),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textPrimary,
                )
                AppButton(stringResource(Res.string.audio_providers_home_action), onClick = onOpenProviders)
            }
        }
    }
}
