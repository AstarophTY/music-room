package com.musicroom.app.player

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.spotify_required_body
import com.musicroom.app.generated.resources.spotify_required_title
import com.musicroom.app.music.SpotifyConnectCard
import com.musicroom.app.theme.components.AppBottomSheet
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import org.jetbrains.compose.resources.stringResource

/**
 * Shown when the user tries to play or queue a song without a linked Spotify account: explains why and lets them
 * link it right there. [onConnected] runs once the account is linked (the sheet then closes).
 */
@Composable
fun SpotifyRequiredSheet(api: MusicRoomApi, onDismiss: () -> Unit, onConnected: () -> Unit) {
    val colors = AppTheme.colors
    AppBottomSheet(onDismiss) { close ->
        Text(stringResource(Res.string.spotify_required_title), style = AppTheme.typography.title, color = colors.textPrimary)
        Text(stringResource(Res.string.spotify_required_body), style = AppTheme.typography.body, color = colors.textSecondary)
        SpotifyConnectCard(
            api,
            onConnected = {
                onConnected()
                close()
            },
        )
    }
}
