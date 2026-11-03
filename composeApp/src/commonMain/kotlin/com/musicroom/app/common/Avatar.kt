package com.musicroom.app.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicroom.app.theme.tokens.AppTheme

/** Round avatar with the user's initials. */
@Composable
fun Avatar(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val colors = AppTheme.colors
    val initials = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
    Box(
        modifier = modifier.size(size).background(colors.accentSubtleStrong, CircleShape)
            .border(AppTheme.spacing.border, colors.accentBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            style = AppTheme.typography.label.merge(TextStyle(fontSize = (size.value * 0.38f).sp)),
            color = colors.accentText,
        )
    }
}
