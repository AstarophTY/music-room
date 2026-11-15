package com.musicroom.app.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme

/** An event on the map: its name over the dot standing on the spot it happens at. */
@Composable
fun EventMapPin(name: String, playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Column(
        modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            name,
            style = AppTheme.typography.label,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 140.dp)
                .background(colors.surface, AppShapes.small)
                .border(1.dp, colors.border, AppShapes.small)
                .padding(horizontal = AppTheme.spacing.sm, vertical = 2.dp),
        )
        // An event whose music runs is the one worth joining now: it wears the accent, the others stay quiet.
        Box(
            Modifier.size(16.dp)
                .background(if (playing) colors.accent else colors.surface, CircleShape)
                .border(2.dp, if (playing) colors.accentBorder else colors.borderStrong, CircleShape),
        )
    }
}
