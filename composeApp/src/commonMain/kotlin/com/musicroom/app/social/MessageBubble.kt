package com.musicroom.app.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicroom.app.theme.tokens.AppTheme

/**
 * One chat message: amber on the right when [mine], gray on the left otherwise. [footer] (time, "Seen") shows
 * under the last bubble of a group.
 */
@Composable
fun MessageBubble(text: String, mine: Boolean, footer: String?, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val corner = 18.dp
    val tail = 4.dp
    val shape = if (mine) {
        RoundedCornerShape(corner, corner, if (footer != null) tail else corner, corner)
    } else {
        RoundedCornerShape(corner, corner, corner, if (footer != null) tail else corner)
    }
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text,
            style = AppTheme.typography.body,
            color = if (mine) colors.onAccent else colors.textPrimary,
            modifier = Modifier.widthIn(max = 320.dp)
                .background(if (mine) colors.accent else colors.surfaceSubtle, shape)
                .padding(horizontal = 14.dp, vertical = AppTheme.spacing.sm),
        )
        if (footer != null) {
            Text(
                footer,
                style = AppTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = AppTheme.spacing.xs),
            )
        }
    }
}
