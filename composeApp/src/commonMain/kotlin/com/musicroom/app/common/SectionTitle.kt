package com.musicroom.app.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.musicroom.app.theme.tokens.AppTheme

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = AppTheme.typography.heading, color = AppTheme.colors.textPrimary)
}
