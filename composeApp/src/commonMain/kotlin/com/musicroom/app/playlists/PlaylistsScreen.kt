package com.musicroom.app.playlists

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.musicroom.app.common.EmptyState
import com.musicroom.app.common.TabContent
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.home_playlists_title
import com.musicroom.app.generated.resources.playlists_create
import com.musicroom.app.generated.resources.playlists_empty_body
import com.musicroom.app.generated.resources.playlists_empty_title
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlaylistsScreen(onCreate: () -> Unit) {
    TabContent {
        Text(
            stringResource(Res.string.home_playlists_title),
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
        )
        EmptyState(
            icon = Lucide.ListMusic,
            title = stringResource(Res.string.playlists_empty_title),
            body = stringResource(Res.string.playlists_empty_body),
            actionLabel = stringResource(Res.string.playlists_create),
            onAction = onCreate,
        )
    }
}
