package com.musicroom.app.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.signup_step
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/** "Step 2 of 3" and a segmented bar. [step] is 1-based. */
@Composable
fun StepProgress(step: Int, stepCount: Int) {
    val colors = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        Text(stringResource(Res.string.signup_step, step, stepCount), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
            repeat(stepCount) { index ->
                Box(
                    Modifier.weight(1f).height(6.dp)
                        .background(if (index < step) colors.accent else colors.border, AppShapes.pill),
                )
            }
        }
    }
}
