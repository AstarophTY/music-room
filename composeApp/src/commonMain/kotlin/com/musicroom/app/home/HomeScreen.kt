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

/**
 * Overview of the app: what is happening now. The greeting, a shortcut to start an event, then the public
 * rooms open now, the tracks picked for the user and their playlists, each linking to its detailed tab.
 */
@Composable
fun HomeScreen(
    api: MusicRoomApi,
    user: UserResponse,
    player: PlayerController,
    onCreate: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenEvents: () -> Unit,
    onOpenRoomPlaylist: (String) -> Unit,
    onOpenPlaylists: () -> Unit,
) {
    TabContent {
        Text(
            stringResource(Res.string.home_greeting, user.displayName),
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
        )
        HomeSearchShortcut(onOpen = onOpenSearch)
        HeroCard(onStartEvent = onCreate)
        HomeRoomsSection(api, onOpenRoom = onOpenEvent, onSeeAll = onOpenEvents)
        if (user.spotifyConnected) {
            RecommendationsSection(api, player)
        }
        HomePlaylistsSection(api, onOpenPlaylist = onOpenRoomPlaylist, onSeeAll = onOpenPlaylists)
    }
}
