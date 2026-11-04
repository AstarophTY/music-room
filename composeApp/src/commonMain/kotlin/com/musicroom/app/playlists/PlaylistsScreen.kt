package com.musicroom.app.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.musicroom.app.common.EmptyState
import com.musicroom.app.common.SectionTitle
import com.musicroom.app.common.TabContent
import com.musicroom.app.common.UiText
import com.musicroom.app.common.WindowSize
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.common_retry
import com.musicroom.app.generated.resources.home_playlists_title
import com.musicroom.app.generated.resources.playlists_create
import com.musicroom.app.generated.resources.playlists_empty_body
import com.musicroom.app.generated.resources.playlists_empty_title
import com.musicroom.app.generated.resources.playlists_mine
import com.musicroom.app.generated.resources.playlists_spotify
import com.musicroom.app.generated.resources.playlists_spotify_empty
import com.musicroom.app.music.SpotifyConnectCard
import com.musicroom.app.player.PlayerController
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PlaylistSummaryResponse
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.stringResource

/**
 * The playlists made in Music Room (created from the Create tab), then the ones the user created on their Spotify
 * account. Opening one shows its tracks in place of the list.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlaylistsScreen(
    api: MusicRoomApi,
    user: UserResponse,
    player: PlayerController,
    onSpotifyConnected: () -> Unit,
    onCreate: () -> Unit,
) {
    val colors = AppTheme.colors
    var spotifyPlaylists by remember { mutableStateOf<List<PlaylistSummaryResponse>?>(null) }
    var error by remember { mutableStateOf<UiText?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(user.spotifyConnected, attempt) {
        if (!user.spotifyConnected) return@LaunchedEffect
        error = null
        try {
            spotifyPlaylists = api.myPlaylists().items
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
        }
    }

    val open = openId?.let { id -> spotifyPlaylists?.firstOrNull { it.id == id } }
    BackHandler(enabled = open != null) { openId = null }
    if (open != null) return PlaylistDetail(api, open, player, onBack = { openId = null })

    TabContent {
        Text(stringResource(Res.string.home_playlists_title), style = AppTheme.typography.display, color = colors.textPrimary)

        SectionTitle(stringResource(Res.string.playlists_mine))
        EmptyState(
            icon = Lucide.ListMusic,
            title = stringResource(Res.string.playlists_empty_title),
            body = stringResource(Res.string.playlists_empty_body),
            actionLabel = stringResource(Res.string.playlists_create),
            onAction = onCreate,
        )

        val loaded = spotifyPlaylists
        SectionTitle(
            stringResource(Res.string.playlists_spotify) + (loaded?.takeIf { user.spotifyConnected }?.let { " · ${it.size}" } ?: ""),
        )
        val failure = error
        when {
            !user.spotifyConnected -> SpotifyConnectCard(api, onConnected = onSpotifyConnected)
            failure != null -> {
                Text(failure.resolve(), style = AppTheme.typography.bodySmall, color = colors.error)
                AppButton(stringResource(Res.string.common_retry), { attempt++ }, variant = AppButtonVariant.Secondary)
            }
            loaded == null -> Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            loaded.isEmpty() -> Text(stringResource(Res.string.playlists_spotify_empty), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            else -> {
                // Two columns on tablets and computers.
                val columns = if (WindowSize.current == WindowSize.Compact) 1 else 2
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                    loaded.chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)) {
                            row.forEach { playlist ->
                                PlaylistRow(api, playlist, onClick = { openId = playlist.id }, modifier = Modifier.weight(1f))
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}
