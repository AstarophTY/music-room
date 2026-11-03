package com.musicroom.app.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_back
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Full-screen page with a back arrow and a title (profile, settings). Centered column on large screens. */
@Composable
fun SubScreenScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = AppTheme.colors
    Box(
        Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.fillMaxWidth().widthIn(max = 680.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(AppTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Lucide.ArrowLeft, contentDescription = stringResource(Res.string.common_back), tint = colors.textPrimary)
                }
                Text(title, style = AppTheme.typography.heading, color = colors.textPrimary)
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.lg, vertical = AppTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                content = content,
            )
        }
    }
}
