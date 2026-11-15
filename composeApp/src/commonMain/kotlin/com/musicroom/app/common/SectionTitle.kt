package com.musicroom.app.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.musicroom.app.theme.tokens.AppTheme

/** Section heading; [color] lets the player draw it white over its blurred cover. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, color: Color = AppTheme.colors.textPrimary) {
    Text(text, modifier = modifier, style = AppTheme.typography.heading, color = color)
}
