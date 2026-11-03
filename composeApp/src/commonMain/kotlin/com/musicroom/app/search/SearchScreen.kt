package com.musicroom.app.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.AudioLines
import com.composables.icons.lucide.Disc3
import com.composables.icons.lucide.Guitar
import com.composables.icons.lucide.Headphones
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.Piano
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Sparkles
import com.musicroom.app.common.SectionTitle
import com.musicroom.app.common.TabContent
import com.musicroom.app.common.WindowSize
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.genre_chill
import com.musicroom.app.generated.resources.genre_electro
import com.musicroom.app.generated.resources.genre_hiphop
import com.musicroom.app.generated.resources.genre_jazz
import com.musicroom.app.generated.resources.genre_pop
import com.musicroom.app.generated.resources.genre_rock
import com.musicroom.app.generated.resources.search_browse
import com.musicroom.app.generated.resources.search_placeholder
import com.musicroom.app.generated.resources.search_soon_body
import com.musicroom.app.generated.resources.search_soon_title
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Music search. The catalog is not connected yet, so it shows the field, a notice and moods to browse later. */
@Composable
fun SearchScreen() {
    var query by rememberSaveable { mutableStateOf("") }
    val colors = AppTheme.colors
    val moods = listOf(
        Res.string.genre_pop to Lucide.Sparkles,
        Res.string.genre_rock to Lucide.Guitar,
        Res.string.genre_hiphop to Lucide.Mic,
        Res.string.genre_electro to Lucide.AudioLines,
        Res.string.genre_jazz to Lucide.Piano,
        Res.string.genre_chill to Lucide.Headphones,
    )
    val tileColors = listOf(colors.accentSubtle, colors.accentSubtleStrong, colors.surfaceSubtle)

    TabContent {
        AppTextField(
            query, { query = it }, stringResource(Res.string.search_placeholder), Modifier.fillMaxWidth(),
            leadingIcon = Lucide.Search,
        )
        Row(
            Modifier.fillMaxWidth().background(colors.accentSubtle, AppShapes.large).padding(AppTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Lucide.Disc3, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(Res.string.search_soon_title), style = AppTheme.typography.label, color = colors.textPrimary)
                Text(stringResource(Res.string.search_soon_body), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        SectionTitle(stringResource(Res.string.search_browse))
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            val columns = if (WindowSize.current == WindowSize.Compact) 2 else 3
            moods.chunked(columns).forEachIndexed { row, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    pair.forEachIndexed { column, (label, icon) ->
                        Box(
                            Modifier.weight(1f).height(96.dp)
                                .background(tileColors[(row * columns + column) % tileColors.size], AppShapes.large)
                                .padding(AppTheme.spacing.md),
                        ) {
                            Text(stringResource(label), style = AppTheme.typography.heading, color = colors.textPrimary)
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = colors.accentText,
                                modifier = Modifier.align(Alignment.BottomEnd).size(32.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
