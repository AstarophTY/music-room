package com.musicroom.app.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.musicroom.app.common.Reveal
import com.musicroom.app.common.TabContent
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.home_greeting
import com.musicroom.app.music.ProviderSession
import com.musicroom.app.player.PlayerController
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UserResponse
import org.jetbrains.compose.resources.stringResource

/**
 * Overview of the app: what is happening now. The greeting, then shelves of square cards
 * (public rooms, tracks picked for the user, their playlists), each linking to its detailed tab.
 */
@Composable
fun HomeScreen(
    api: MusicRoomApi,
    user: UserResponse,
    player: PlayerController,
    onOpenEvent: (String) -> Unit,
    onOpenEvents: () -> Unit,
    onOpenRoomPlaylist: (String) -> Unit,
    onOpenPlaylists: () -> Unit,
) {
    TabContent {
        val providers = ProviderSession.connections.current
        if (com.musicroom.app.common.WindowSize.current != com.musicroom.app.common.WindowSize.Compact) {
            Text(
                stringResource(Res.string.home_greeting, user.displayName),
                style = AppTheme.typography.display,
                color = AppTheme.colors.textPrimary,
            )
        }
        Reveal(0) { HomeRoomsSection(api, onOpenRoom = onOpenEvent, onSeeAll = onOpenEvents) }
        if (providers.any { it.available && it.connected && it.providesRecommendations }) {
            Reveal(1) { RecommendationsSection(api, player) }
        }
        Reveal(2) { HomePlaylistsSection(api, onOpenPlaylist = onOpenRoomPlaylist, onSeeAll = onOpenPlaylists) }
    }
}
