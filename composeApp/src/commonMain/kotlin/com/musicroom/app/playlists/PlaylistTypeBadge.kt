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
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Users
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.playlist_type_personal
import com.musicroom.app.generated.resources.playlist_type_shared
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.dto.PlaylistType
import org.jetbrains.compose.resources.stringResource

/** Small pill saying whether a playlist is personal or shared. */
@Composable
fun PlaylistTypeBadge(type: PlaylistType, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val shared = type == PlaylistType.SHARED
    Row(
        modifier.background(if (shared) colors.accentSubtle else colors.surfaceSubtle, AppShapes.pill)
            .padding(horizontal = AppTheme.spacing.sm, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (shared) Lucide.Users else Lucide.Lock,
            contentDescription = null,
            tint = if (shared) colors.accentText else colors.textSecondary,
            modifier = Modifier.size(12.dp),
        )
        Text(
            stringResource(if (shared) Res.string.playlist_type_shared else Res.string.playlist_type_personal),
            style = AppTheme.typography.bodySmall,
            color = if (shared) colors.accentText else colors.textSecondary,
        )
    }
}
