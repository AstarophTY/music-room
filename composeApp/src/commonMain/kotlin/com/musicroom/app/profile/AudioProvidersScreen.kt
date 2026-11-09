package com.musicroom.app.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.AudioLines
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Lucide
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.audio_providers_title
import com.musicroom.app.generated.resources.provider_connected
import com.musicroom.app.generated.resources.provider_connected_as
import com.musicroom.app.generated.resources.provider_disconnect
import com.musicroom.app.generated.resources.provider_playback_note
import com.musicroom.app.music.MusicProviderConnection
import com.musicroom.app.music.ProviderConnectCard
import com.musicroom.app.music.ProviderSession
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.dto.TrackSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Manages every music provider through the common connection contract. */
@Composable
fun AudioProvidersScreen(onBack: () -> Unit) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val providers = ProviderSession.connections.current
    val onProviderChanged = ProviderSession.onProviderChanged.current
    var busy by remember { mutableStateOf<Set<TrackSource>>(emptySet()) }
    var error by remember { mutableStateOf<UiText?>(null) }
    var accountLabels by remember { mutableStateOf<Map<TrackSource, String>>(emptyMap()) }

    LaunchedEffect(providers.map { it.source to it.connected }) {
        accountLabels = providers.filter { it.connected }.mapNotNull { provider ->
            val label = try {
                provider.accountLabel()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            label?.let { provider.source to it }
        }.toMap()
    }

    fun disconnect(provider: MusicProviderConnection) {
        if (provider.source in busy) return
        error = null
        scope.launch {
            busy = busy + provider.source
            try {
                provider.disconnect()
                onProviderChanged()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
            } finally {
                busy = busy - provider.source
            }
        }
    }

    SubScreenScaffold(title = stringResource(Res.string.audio_providers_title), onBack = onBack) {
        providers.filter { it.available }.forEach { provider ->
            if (provider.connected) {
                AppCard(Modifier.fillMaxWidth()) {
                    SettingsRow(
                        Lucide.AudioLines,
                        accountLabels[provider.source]?.let { stringResource(Res.string.provider_connected_as, it) }
                            ?: stringResource(Res.string.provider_connected),
                        onClick = {},
                        showChevron = false,
                    )
                    SettingsRow(
                        Lucide.LogOut,
                        stringResource(Res.string.provider_disconnect, provider.displayName),
                        onClick = { disconnect(provider) },
                        destructive = true,
                        showChevron = false,
                    )
                }
            } else {
                ProviderConnectCard(provider, onConnected = onProviderChanged)
            }
        }
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
        Text(stringResource(Res.string.provider_playback_note), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
    }
}
