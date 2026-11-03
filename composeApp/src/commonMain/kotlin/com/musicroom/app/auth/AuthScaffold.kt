package com.musicroom.app.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.musicroom.app.common.Equalizer
import com.musicroom.app.common.WindowSize
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.app_name
import com.musicroom.app.generated.resources.common_back
import com.musicroom.app.generated.resources.signup_step
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Common layout of the auth screens: brand mark or back arrow, step progress, title, subtitle, then [content].
 * On large screens, a yellow brand panel takes the left half.
 * [step] is 1-based; pass null to hide the progress bar.
 */
@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    backEnabled: Boolean = true,
    step: Int? = null,
    stepCount: Int = 0,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val spacing = AppTheme.spacing

    val expanded = WindowSize.current == WindowSize.Expanded
    Row(Modifier.fillMaxSize().background(colors.background)) {
        if (expanded) AuthBrandPanel(Modifier.weight(1f).fillMaxHeight())
        Box(
            Modifier.weight(1f).fillMaxHeight().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()),
            contentAlignment = if (expanded) Alignment.Center else Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)
                    .padding(horizontal = spacing.lg, vertical = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                AuthHeader(onBack, backEnabled)
                if (step != null) StepProgress(step, stepCount)
                Text(title, style = typography.display, color = colors.textPrimary)
                Text(subtitle, style = typography.body, color = colors.textSecondary)
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.md), content = content)
            }
        }
    }
}
