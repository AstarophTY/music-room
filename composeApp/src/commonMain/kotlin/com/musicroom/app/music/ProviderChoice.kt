package com.musicroom.app.music

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.provider_choice_body
import com.musicroom.app.generated.resources.provider_choice_later
import com.musicroom.app.generated.resources.provider_choice_listener
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Lists the providers that can be linked on this platform, using only their common connection contract. */
@Composable
fun ProviderChoice(onProviderChanged: () -> Unit, onContinue: () -> Unit, listenerOption: Boolean = true) {
    val providers = ProviderSession.connections.current
    Text(stringResource(Res.string.provider_choice_body), style = AppTheme.typography.body, color = AppTheme.colors.textPrimary)
    providers.filter { it.available && !it.connected }.forEach { provider ->
        ProviderConnectCard(provider, onConnected = onProviderChanged)
    }
    AppButton(
        stringResource(if (listenerOption) Res.string.provider_choice_listener else Res.string.provider_choice_later),
        onClick = onContinue,
        variant = AppButtonVariant.Ghost,
    )
}
