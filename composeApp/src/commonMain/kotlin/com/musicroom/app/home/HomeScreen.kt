package com.musicroom.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.musicroom.app.common.EmptyState
import com.musicroom.app.common.TabContent
import com.musicroom.app.common.WindowSize
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.events_create
import com.musicroom.app.generated.resources.home_greeting
import com.musicroom.app.generated.resources.home_live_empty
import com.musicroom.app.generated.resources.home_live_title
import com.musicroom.app.generated.resources.home_playlists_empty
import com.musicroom.app.generated.resources.home_playlists_title
import com.musicroom.app.generated.resources.playlists_create
import com.musicroom.app.music.SpotifyConnectCard
import com.musicroom.app.player.PlayerController
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UserResponse
import org.jetbrains.compose.resources.stringResource

/** Greeting, the "start an event" card, Spotify recommendations (or the card to connect Spotify), then events and playlists. */
@Composable
fun HomeScreen(
    api: MusicRoomApi,
    user: UserResponse,
    player: PlayerController,
    onSpotifyConnected: () -> Unit,
    onCreate: () -> Unit,
    onOpenPlaylists: () -> Unit,
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
        val live: @Composable (Modifier) -> Unit = { modifier ->
            EmptyState(
                icon = Lucide.MapPin,
                title = stringResource(Res.string.home_live_title),
                body = stringResource(Res.string.home_live_empty),
                modifier = modifier,
                actionLabel = stringResource(Res.string.events_create),
                onAction = onCreate,
            )
        }
        val playlists: @Composable (Modifier) -> Unit = { modifier ->
            EmptyState(
                icon = Lucide.ListMusic,
                title = stringResource(Res.string.home_playlists_title),
                body = stringResource(Res.string.home_playlists_empty),
                modifier = modifier,
                actionLabel = stringResource(Res.string.playlists_create),
                onAction = onOpenPlaylists,
            )
        }
        if (WindowSize.current == WindowSize.Compact) {
            live(Modifier)
            playlists(Modifier)
        } else {
            // Side by side on tablets and computers.
            Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                live(Modifier.weight(1f).fillMaxHeight())
                playlists(Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}
