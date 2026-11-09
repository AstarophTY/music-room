package com.musicroom.app.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.AudioLines
import com.composables.icons.lucide.Lucide
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.spotify_connect_body
import com.musicroom.app.generated.resources.spotify_connect_button
import com.musicroom.app.generated.resources.spotify_connect_title
import com.musicroom.app.generated.resources.spotify_connect_waiting
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.theme.tokens.AppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Asks to link Spotify. The Spotify login opens in the browser and sends the user back to the server, so the
 * app checks the status every few seconds until the account shows up, then calls [onConnected].
 */
@Composable
fun SpotifyConnectCard(provider: MusicProviderConnection, onConnected: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var waiting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }

    AppCard(modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp).background(colors.accentSubtle, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Lucide.AudioLines, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(Res.string.spotify_connect_title), style = AppTheme.typography.heading, color = colors.textPrimary)
                Text(stringResource(Res.string.spotify_connect_body), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        AppButton(
            stringResource(Res.string.spotify_connect_button),
            onClick = {
                error = null
                scope.launch {
                    try {
                        waiting = true
                        if (provider.connect()) onConnected()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error = UiText.of(e)
                    } finally {
                        waiting = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = AppTheme.spacing.md),
            enabled = !waiting,
            icon = Lucide.AudioLines,
        )
        if (waiting) {
            Text(
                stringResource(Res.string.spotify_connect_waiting),
                style = AppTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = AppTheme.spacing.sm),
            )
        }
        error?.let {
            Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error, modifier = Modifier.padding(top = AppTheme.spacing.sm))
        }
    }
}
