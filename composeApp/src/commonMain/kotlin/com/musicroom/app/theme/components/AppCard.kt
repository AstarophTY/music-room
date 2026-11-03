package com.musicroom.app.theme.components

import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier.background(AppTheme.colors.surface, AppShapes.medium)
        .border(AppTheme.spacing.border, AppTheme.colors.border, AppShapes.medium)
        .padding(AppTheme.spacing.md)) { content() }
}
