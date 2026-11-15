package com.musicroom.app.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.musicroom.app.common.SectionTitle
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.search_see_all
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Title of a home section, with a "See all" shortcut to its tab when [onSeeAll] is set. */
@Composable
fun HomeSectionHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionTitle(title, Modifier.weight(1f))
        if (onSeeAll != null) {
            Text(
                stringResource(Res.string.search_see_all),
                style = AppTheme.typography.label,
                color = AppTheme.colors.accentText,
                modifier = Modifier.pressable(onClick = onSeeAll).padding(AppTheme.spacing.xs),
            )
        }
    }
}
