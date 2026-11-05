package com.musicroom.app.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.musicroom.app.common.Equalizer
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.devices_active
import com.musicroom.app.generated.resources.devices_empty
import com.musicroom.app.generated.resources.devices_refresh
import com.musicroom.app.generated.resources.devices_title
import com.musicroom.app.generated.resources.spotify_open
import com.musicroom.app.theme.components.AppBottomSheet
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Card from the bottom listing the user's devices with Spotify open (phone, computer, speaker...). The one
 * playing is highlighted; tapping another moves the music there and closes the card. With none, it explains
 * how to make one appear.
 */
@Composable
fun DevicePickerSheet(player: PlayerController, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<UiText?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        loading = true
        error = null
        try {
            player.loadDevices()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
        }
        loading = false
    }

    AppBottomSheet(onDismiss) { close ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.devices_title), style = AppTheme.typography.title, color = colors.textPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = { attempt++ }, enabled = !loading) {
                Icon(Lucide.RefreshCw, contentDescription = stringResource(Res.string.devices_refresh), tint = colors.textSecondary)
            }
        }
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
        val devices = player.devices
        when {
            devices == null && loading ->
                Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            devices.isNullOrEmpty() -> {
                Text(stringResource(Res.string.devices_empty), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                AppButton(
                    stringResource(Res.string.spotify_open),
                    { runCatching { uriHandler.openUri(PlayerController.SPOTIFY_WEB_URL) } },
                    variant = AppButtonVariant.Secondary,
                )
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                devices.forEach { device ->
                    val background by animateColorAsState(if (device.isActive) colors.accentSubtle else Color.Transparent)
                    Row(
                        Modifier.fillMaxWidth().pressable(onClick = {
                            if (device.isActive) {
                                close()
                            } else {
                                player.transferTo(device)
                                scope.launch {
                                    delay(400)
                                    close()
                                }
                            }
                        }).background(background, AppShapes.medium).padding(AppTheme.spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(40.dp).background(if (device.isActive) colors.accent else colors.surfaceSubtle, AppShapes.medium),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                device.type.icon(),
                                contentDescription = null,
                                tint = if (device.isActive) colors.onAccent else colors.textPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                device.name,
                                style = AppTheme.typography.label,
                                color = if (device.isActive) colors.accentText else colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (device.isActive) {
                                Text(stringResource(Res.string.devices_active), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                        }
                        if (device.isActive && player.isPlaying) Equalizer(color = colors.accentText, height = 16.dp)
                    }
                }
            }
        }
    }
}
