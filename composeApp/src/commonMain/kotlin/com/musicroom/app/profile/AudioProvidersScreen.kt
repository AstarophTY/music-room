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
import com.musicroom.app.generated.resources.apple_music_connect
import com.musicroom.app.generated.resources.apple_music_connected
import com.musicroom.app.generated.resources.apple_music_connecting
import com.musicroom.app.generated.resources.apple_music_not_connected
import com.musicroom.app.generated.resources.audio_providers_title
import com.musicroom.app.generated.resources.spotify_connected
import com.musicroom.app.generated.resources.spotify_connected_as
import com.musicroom.app.generated.resources.spotify_disconnect
import com.musicroom.app.generated.resources.spotify_playback_note
import com.musicroom.app.generated.resources.spotify_title
import com.musicroom.app.music.SpotifyConnectCard
import com.musicroom.app.music.AppleMusicConnection
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.SpotifyStatusResponse
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Manages music provider connections. */
@Composable
fun AudioProvidersScreen(api: MusicRoomApi, user: UserResponse, onUserChange: (UserResponse) -> Unit, onBack: () -> Unit) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val appleMusic = AppleMusicConnection.Local.current
    var status by remember { mutableStateOf<SpotifyStatusResponse?>(null) }
    var error by remember { mutableStateOf<UiText?>(null) }
    var connecting by remember { mutableStateOf(false) }
    LaunchedEffect(user.spotifyConnected) {
        status = try {
            api.spotifyStatus()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    SubScreenScaffold(title = stringResource(Res.string.audio_providers_title), onBack = onBack) {
        if (!user.spotifyConnected) {
            SpotifyConnectCard(api, onConnected = { onUserChange(user.copy(spotifyConnected = true)) })
        } else {
            AppCard(Modifier.fillMaxWidth()) {
                SettingsRow(
                    Lucide.AudioLines,
                    status?.displayName?.let { stringResource(Res.string.spotify_connected_as, it) }
                        ?: stringResource(Res.string.spotify_connected),
                    onClick = {},
                    showChevron = false,
                )
                SettingsRow(
                    Lucide.LogOut,
                    stringResource(Res.string.spotify_disconnect),
                    onClick = {
                        error = null
                        scope.launch {
                            try {
                                onUserChange(api.spotifyDisconnect())
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                error = UiText.of(e)
                            }
                        }
                    },
                    destructive = true,
                    showChevron = false,
                )
            }
        }
        if (appleMusic.available) {
            AppCard(Modifier.fillMaxWidth()) {
                SettingsRow(
                    Lucide.AudioLines,
                    "Apple Music",
                    onClick = {
                        error = null
                        if (connecting) return@SettingsRow
                        if (appleMusic.connected) {
                            appleMusic.disconnect()
                        } else {
                            scope.launch {
                                connecting = true
                                try {
                                    if (!appleMusic.connect(api.appleMusicDeveloperToken().token)) {
                                        error = UiText.of(Res.string.apple_music_not_connected)
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    error = UiText.of(e)
                                } finally {
                                    connecting = false
                                }
                            }
                        }
                    },
                    value = stringResource(if (connecting) Res.string.apple_music_connecting else if (appleMusic.connected) Res.string.apple_music_connected else Res.string.apple_music_connect),
                )
            }
        }
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
        Text(stringResource(Res.string.spotify_playback_note), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
    }
}
