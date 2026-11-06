package com.musicroom.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.search_placeholder
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Search field look-alike on the home page, opening the search tab. */
@Composable
fun HomeSearchShortcut(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Row(
        modifier.fillMaxWidth().clip(AppShapes.pill).background(colors.surface).pressable(onClick = onOpen)
            .padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Lucide.Search, contentDescription = null, tint = colors.textSecondary)
        Text(stringResource(Res.string.search_placeholder), style = AppTheme.typography.body, color = colors.textSecondary)
    }
}
