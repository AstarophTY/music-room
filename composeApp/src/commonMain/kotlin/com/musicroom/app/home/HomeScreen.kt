package com.musicroom.app.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.musicroom.app.common.TabContent
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.home_greeting
import com.musicroom.app.music.SpotifyConnectCard
import com.musicroom.app.player.PlayerController
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UserResponse
import org.jetbrains.compose.resources.stringResource

/** Greeting, the "start an event" card, then Spotify recommendations (or the card to connect Spotify). */
@Composable
fun HomeScreen(
    api: MusicRoomApi,
    user: UserResponse,
    player: PlayerController,
    onSpotifyConnected: () -> Unit,
    onCreate: () -> Unit,
) {
    TabContent {
        Text(
            stringResource(Res.string.home_greeting, user.displayName),
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
        )
        HeroCard(onStartEvent = onCreate)
        if (user.spotifyConnected) {
            RecommendationsSection(api, player)
        } else {
            SpotifyConnectCard(api, onConnected = onSpotifyConnected)
        }
    }
}
