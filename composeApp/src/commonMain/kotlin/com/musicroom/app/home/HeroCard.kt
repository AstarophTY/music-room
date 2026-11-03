package com.musicroom.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Radio
import com.musicroom.app.common.Equalizer
import com.musicroom.app.common.VinylRings
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.home_hero_body
import com.musicroom.app.generated.resources.home_hero_button
import com.musicroom.app.generated.resources.home_hero_title
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Big yellow card on the home tab, with vinyl grooves in the corner and a live equalizer. */
@Composable
fun HeroCard(onStartEvent: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Box(modifier.fillMaxWidth().clip(AppShapes.large).background(colors.accent)) {
        VinylRings(colors.onAccent, Modifier.matchParentSize())
        Column(
            Modifier.padding(AppTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            Equalizer(color = colors.onAccent, height = 28.dp)
            Text(stringResource(Res.string.home_hero_title), style = AppTheme.typography.title, color = colors.onAccent)
            Text(
                stringResource(Res.string.home_hero_body),
                style = AppTheme.typography.body,
                color = colors.onAccent.copy(alpha = 0.75f),
            )
            AppButton(
                text = stringResource(Res.string.home_hero_button),
                onClick = onStartEvent,
                variant = AppButtonVariant.OnAccent,
                icon = Lucide.Radio,
            )
        }
    }
}
