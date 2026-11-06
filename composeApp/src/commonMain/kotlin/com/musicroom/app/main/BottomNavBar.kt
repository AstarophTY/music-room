package com.musicroom.app.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.musicroom.app.common.UserAvatar
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UserResponse

/** Five tabs; events is a raised yellow disc that spins while it is selected. Settings shows the user's avatar. */
@Composable
fun BottomNavBar(
    api: MusicRoomApi,
    user: UserResponse,
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
) {
    val colors = AppTheme.colors
    val border = AppTheme.spacing.border
    Row(
        Modifier.fillMaxWidth()
            .background(colors.surface)
            .drawBehind { drawLine(colors.border, Offset(0f, 0f), Offset(size.width, 0f), border.toPx()) }
            .navigationBarsPadding()
            .height(72.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MainTab.entries.forEach { tab ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (tab == MainTab.Events) {
                    EventsDisc(selected = selected == tab, onClick = { onSelect(tab) })
                } else {
                    NavItem(
                        tab = tab,
                        selected = selected == tab,
                        onClick = { onSelect(tab) },
                        avatar = if (tab == MainTab.Settings) {
                            { UserAvatar(api, user, size = 22.dp) }
                        } else null,
                    )
                }
            }
        }
    }
}
