package com.musicroom.app.create

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.SlidersHorizontal
import com.composables.icons.lucide.Vote
import com.musicroom.app.common.TabContent
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.create_control_body
import com.musicroom.app.generated.resources.create_control_title
import com.musicroom.app.generated.resources.create_event_body
import com.musicroom.app.generated.resources.create_event_title
import com.musicroom.app.generated.resources.create_playlist_body
import com.musicroom.app.generated.resources.create_playlist_title
import com.musicroom.app.generated.resources.create_title
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Entry point to start one of the three services of the project. */
@Composable
fun CreateScreen() {
    TabContent {
        Text(stringResource(Res.string.create_title), style = AppTheme.typography.display, color = AppTheme.colors.textPrimary)
        CreateOptionCard(Lucide.Vote, stringResource(Res.string.create_event_title), stringResource(Res.string.create_event_body))
        CreateOptionCard(Lucide.ListMusic, stringResource(Res.string.create_playlist_title), stringResource(Res.string.create_playlist_body))
        CreateOptionCard(
            Lucide.SlidersHorizontal,
            stringResource(Res.string.create_control_title),
            stringResource(Res.string.create_control_body),
        )
    }
}
