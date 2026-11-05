package com.musicroom.app.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicroom.app.theme.tokens.AppTheme

/** Scrollable content of a tab. On large screens it stays centered at a readable width. */
@Composable
fun TabContent(content: @Composable ColumnScope.() -> Unit) {
    val wide = WindowSize.current != WindowSize.Compact
    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 1040.dp).padding(
                horizontal = if (wide) AppTheme.spacing.xl else AppTheme.spacing.md,
                vertical = if (wide) AppTheme.spacing.lg else AppTheme.spacing.sm,
            ),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
            content = content,
        )
    }
}
