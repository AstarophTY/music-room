package com.musicroom.app.music

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.apple_music_connect
import com.musicroom.app.generated.resources.apple_music_connecting
import com.musicroom.app.generated.resources.apple_music_not_connected
import com.musicroom.app.generated.resources.provider_apple_catalog_pending
import com.musicroom.app.generated.resources.provider_apple_unavailable
import com.musicroom.app.generated.resources.provider_choice_body
import com.musicroom.app.generated.resources.provider_choice_listener
import com.musicroom.app.generated.resources.provider_choice_later
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProviderChoice(api: MusicRoomApi, onSpotifyConnected: () -> Unit, onContinue: () -> Unit, listenerOption: Boolean = true) {
    val apple = AppleMusicConnection.Local.current
    val scope = rememberCoroutineScope()
    var connecting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }
    Text(stringResource(Res.string.provider_choice_body), style = AppTheme.typography.body, color = AppTheme.colors.textPrimary)
    SpotifyConnectCard(api, onConnected = onSpotifyConnected)
    Text("Apple Music", style = AppTheme.typography.heading, color = AppTheme.colors.textPrimary)
    Text(stringResource(if (apple.available) Res.string.provider_apple_catalog_pending else Res.string.provider_apple_unavailable), style = AppTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
    if (apple.available && !apple.connected) {
        AppButton(
            text = stringResource(if (connecting) Res.string.apple_music_connecting else Res.string.apple_music_connect),
            enabled = !connecting,
            onClick = {
                connecting = true
                error = null
                scope.launch {
                    try {
                        if (apple.connect(api.appleMusicDeveloperToken().token)) onContinue()
                        else error = UiText.of(Res.string.apple_music_not_connected)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error = UiText.of(e)
                    } finally {
                        connecting = false
                    }
                }
            },
        )
    }
    error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = AppTheme.colors.error) }
    AppButton(stringResource(if (listenerOption) Res.string.provider_choice_listener else Res.string.provider_choice_later), onClick = onContinue, variant = AppButtonVariant.Ghost)
}
