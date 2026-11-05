package com.musicroom.app.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.musicroom.app.common.TabContent
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.home_greeting
import com.musicroom.app.player.PlayerController
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UserResponse
import org.jetbrains.compose.resources.stringResource

/** Greeting, event creation, and music recommendations or provider settings. */
@Composable
fun HomeScreen(
    api: MusicRoomApi,
    user: UserResponse,
    player: PlayerController,
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
        }
    }
}
