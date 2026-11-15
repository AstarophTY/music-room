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
import com.musicroom.app.generated.resources.provider_connect_body
import com.musicroom.app.generated.resources.provider_connect_button
import com.musicroom.app.generated.resources.provider_connect_title
import com.musicroom.app.generated.resources.provider_connect_waiting
import com.musicroom.app.generated.resources.provider_not_connected
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.theme.tokens.AppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Asks to link one provider. The provider owns its platform flow (browser, SDK, ...), so this card only
 * shows progress and reports the result through [onConnected].
 */
@Composable
fun ProviderConnectCard(
    provider: MusicProviderConnection,
    onConnected: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                ProviderIcon(provider.source, tint = colors.accentText, size = 24.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(Res.string.provider_connect_title, provider.displayName),
                    style = AppTheme.typography.heading,
                    color = colors.textPrimary,
                )
                Text(
                    stringResource(Res.string.provider_connect_body, provider.displayName),
                    style = AppTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
        AppButton(
            stringResource(Res.string.provider_connect_button, provider.displayName),
            onClick = {
                error = null
                scope.launch {
                    try {
                        waiting = true
                        if (provider.connect()) onConnected()
                        else error = UiText.of(Res.string.provider_not_connected, provider.displayName)
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
                stringResource(Res.string.provider_connect_waiting, provider.displayName),
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
