package com.musicroom.app.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.playlist_visibility_private
import com.musicroom.app.generated.resources.playlist_visibility_public
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.dto.PlaylistVisibility
import org.jetbrains.compose.resources.stringResource

/** Small pill saying whether a playlist is public or private. */
@Composable
fun PlaylistVisibilityBadge(visibility: PlaylistVisibility, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val public = visibility == PlaylistVisibility.PUBLIC
    Row(
        modifier.background(if (public) colors.accentSubtle else colors.surfaceSubtle, AppShapes.pill)
            .padding(horizontal = AppTheme.spacing.sm, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (public) Lucide.Globe else Lucide.Lock,
            contentDescription = null,
            tint = if (public) colors.accentText else colors.textSecondary,
            modifier = Modifier.size(12.dp),
        )
        Text(
            stringResource(if (public) Res.string.playlist_visibility_public else Res.string.playlist_visibility_private),
            style = AppTheme.typography.bodySmall,
            color = if (public) colors.accentText else colors.textSecondary,
        )
    }
}
