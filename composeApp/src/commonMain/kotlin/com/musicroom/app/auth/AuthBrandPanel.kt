package com.musicroom.app.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicroom.app.common.Equalizer
import com.musicroom.app.common.VinylRings
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.app_name
import com.musicroom.app.generated.resources.home_hero_body
import com.musicroom.app.generated.resources.home_hero_title
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Yellow half of the auth screens on large screens: brand, tagline, vinyl grooves. */
@Composable
fun AuthBrandPanel(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Box(modifier.background(colors.accent)) {
        VinylRings(colors.onAccent, Modifier.matchParentSize(), rings = 14, spacing = 34.dp)
        Column(
            Modifier.align(Alignment.CenterStart).padding(AppTheme.spacing.xxl).widthIn(max = 520.dp),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                Equalizer(color = colors.onAccent, height = 28.dp)
                Text(stringResource(Res.string.app_name), style = AppTheme.typography.heading, color = colors.onAccent)
            }
            Spacer(Modifier.padding(top = AppTheme.spacing.lg))
            Text(stringResource(Res.string.home_hero_title), style = AppTheme.typography.display, color = colors.onAccent)
            Text(
                stringResource(Res.string.home_hero_body),
                style = AppTheme.typography.body,
                color = colors.onAccent.copy(alpha = 0.75f),
            )
        }
    }
}
