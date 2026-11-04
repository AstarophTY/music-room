package com.musicroom.app.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PanelLeftClose
import com.composables.icons.lucide.PanelLeftOpen
import com.musicroom.app.common.Equalizer
import com.musicroom.app.common.UserAvatar
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.app_name
import com.musicroom.app.generated.resources.profile_open
import com.musicroom.app.generated.resources.sidebar_collapse
import com.musicroom.app.generated.resources.sidebar_expand
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UserResponse
import org.jetbrains.compose.resources.stringResource

/**
 * Side menu that replaces the bottom bar on large screens. The profile stays at the top left.
 * [collapsed] shrinks it to icons only; the button at the bottom toggles it.
 * [selected] is null while a profile page is open: the profile row is highlighted instead of a tab.
 */
@Composable
fun SideNavBar(
    api: MusicRoomApi,
    user: UserResponse,
    selected: MainTab?,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    onSelect: (MainTab) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val colors = AppTheme.colors
    val border = AppTheme.spacing.border
    val width by animateDpAsState(if (collapsed) 72.dp else 256.dp)
    val padding = if (collapsed) 12.dp else AppTheme.spacing.md
    val profileOpen = selected == null
    Column(
        Modifier.width(width).fillMaxHeight().background(colors.surface)
            .drawBehind { drawLine(colors.border, Offset(size.width, 0f), Offset(size.width, size.height), border.toPx()) }
            .clipToBounds()
            .safeDrawingPadding()
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth().pressable(onClick = onOpenProfile)
                .background(if (profileOpen) colors.accentSubtle else Color.Transparent, AppShapes.pill)
                .padding(vertical = AppTheme.spacing.xs, horizontal = if (collapsed) 0.dp else AppTheme.spacing.xs),
            horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(api, user, size = 44.dp)
            if (!collapsed) {
                Column {
                    Text(
                        user.displayName,
                        style = AppTheme.typography.label,
                        color = if (profileOpen) colors.accentText else colors.textPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(Res.string.profile_open),
                        style = AppTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.padding(top = AppTheme.spacing.md))
        MainTab.entries.forEach { tab ->
            SideNavItem(tab, selected = tab == selected, collapsed = collapsed, onClick = { onSelect(tab) })
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.spacedBy(AppTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!collapsed) {
                Equalizer(color = colors.accentText, height = 18.dp, modifier = Modifier.padding(start = AppTheme.spacing.sm))
                Text(
                    stringResource(Res.string.app_name),
                    style = AppTheme.typography.label,
                    color = colors.textSecondary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                )
            }
            IconButton(onClick = onToggleCollapsed) {
                Icon(
                    if (collapsed) Lucide.PanelLeftOpen else Lucide.PanelLeftClose,
                    contentDescription = stringResource(if (collapsed) Res.string.sidebar_expand else Res.string.sidebar_collapse),
                    tint = colors.textSecondary,
                )
            }
        }
    }
}
