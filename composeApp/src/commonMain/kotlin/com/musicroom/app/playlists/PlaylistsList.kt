package com.musicroom.app.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
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
import com.musicroom.app.generated.resources.playlists_from_provider
import com.musicroom.app.generated.resources.playlists_mine
import com.musicroom.app.generated.resources.playlists_provider_empty
import com.musicroom.app.music.MusicPlaylist
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PlaylistResponse
import org.jetbrains.compose.resources.stringResource

/** The playlists tab: Music Room playlists, then one shelf per provider that lists the user's playlists. */
@Composable
fun PlaylistsList(
    api: MusicRoomApi,
    roomPlaylists: List<PlaylistResponse>?,
    roomError: UiText?,
    providerShelves: List<ProviderPlaylists>,
    onRetry: () -> Unit,
    onCreate: () -> Unit,
    onOpenRoom: (String) -> Unit,
    onOpenProviderPlaylist: (MusicPlaylist) -> Unit,
) {
    val colors = AppTheme.colors
    val columns = if (WindowSize.current == WindowSize.Compact) 1 else 2

    @Composable
    fun <T> grid(items: List<T>, row: @Composable (T, Modifier) -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
            items.chunked(columns).forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)) {
                    line.forEach { row(it, Modifier.weight(1f)) }
                    repeat(columns - line.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }

    @Composable
    fun status(error: UiText?, loading: Boolean) {
        if (error != null) {
            Text(error.resolve(), style = AppTheme.typography.bodySmall, color = colors.error)
            AppButton(stringResource(Res.string.common_retry), onRetry, variant = AppButtonVariant.Secondary)
        } else if (loading) {
            Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }

    TabContent {
        if (WindowSize.current != WindowSize.Compact) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.home_playlists_title), Modifier.weight(1f), style = AppTheme.typography.display, color = colors.textPrimary)
                if (!roomPlaylists.isNullOrEmpty()) {
                    AppButton(stringResource(Res.string.playlists_create), onCreate, variant = AppButtonVariant.Secondary, icon = Lucide.Plus)
                }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(Res.string.playlists_mine), Modifier.weight(1f))
            if (WindowSize.current == WindowSize.Compact && !roomPlaylists.isNullOrEmpty()) {
                AppButton(stringResource(Res.string.playlists_create), onCreate, variant = AppButtonVariant.Secondary, icon = Lucide.Plus)
            }
        }
        when {
            roomPlaylists == null -> status(roomError, loading = true)
            roomPlaylists.isEmpty() -> EmptyState(
                icon = Lucide.ListMusic,
                title = stringResource(Res.string.playlists_empty_title),
                body = stringResource(Res.string.playlists_empty_body),
                actionLabel = stringResource(Res.string.playlists_create),
                onAction = onCreate,
            )
            else -> grid(roomPlaylists) { playlist, modifier -> RoomPlaylistRow(api, playlist, onClick = { onOpenRoom(playlist.id) }, modifier = modifier) }
        }

        providerShelves.forEach { shelf ->
            val playlists = shelf.playlists
            SectionTitle(stringResource(Res.string.playlists_from_provider, shelf.provider.displayName) + (playlists?.let { " · ${it.size}" } ?: ""))
            when {
                shelf.error != null -> status(shelf.error, loading = false)
                playlists == null -> status(null, loading = true)
                playlists.isEmpty() -> Text(
                    stringResource(Res.string.playlists_provider_empty, shelf.provider.displayName),
                    style = AppTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                else -> grid(playlists) { playlist, modifier -> PlaylistRow(api, playlist, onClick = { onOpenProviderPlaylist(playlist) }, modifier = modifier) }
            }
        }
    }
}
