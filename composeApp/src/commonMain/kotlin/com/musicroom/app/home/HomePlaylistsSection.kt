package com.musicroom.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.common_retry
import com.musicroom.app.generated.resources.home_playlists_title
import com.musicroom.app.generated.resources.playlists_none_mine
import com.musicroom.app.playlists.RoomPlaylistRow
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PlaylistResponse
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.stringResource

/** Home shortcut to the user's Music Room playlists, with "See all" going to the playlists tab. */
@Composable
fun HomePlaylistsSection(api: MusicRoomApi, onOpenPlaylist: (String) -> Unit, onSeeAll: () -> Unit) {
    val colors = AppTheme.colors
    var playlists by remember { mutableStateOf<List<PlaylistResponse>?>(null) }
    var error by remember { mutableStateOf<UiText?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        error = null
        try {
            playlists = api.roomPlaylists().items
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
        HomeSectionHeader(stringResource(Res.string.home_playlists_title), onSeeAll)
        val loaded = playlists
        val failure = error
        when {
            failure != null -> {
                Text(failure.resolve(), style = AppTheme.typography.bodySmall, color = colors.error)
                AppButton(stringResource(Res.string.common_retry), { attempt++ }, variant = AppButtonVariant.Secondary)
            }
            loaded == null -> Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            loaded.isEmpty() -> Text(stringResource(Res.string.playlists_none_mine), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            // The playlists tab lists them all.
            else -> loaded.take(3).forEach { playlist ->
                RoomPlaylistRow(api, playlist, onClick = { onOpenPlaylist(playlist.id) })
            }
        }
    }
}
