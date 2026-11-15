package com.musicroom.app.auth

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.musicroom.app.generated.resources.app_name
import com.musicroom.app.generated.resources.common_back
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Top of an auth screen: a back arrow, or the brand mark on the first screen. */
@Composable
fun AuthHeader(onBack: (() -> Unit)?, backEnabled: Boolean) {
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack, enabled = backEnabled) {
                Icon(Lucide.ArrowLeft, contentDescription = stringResource(Res.string.common_back), tint = colors.textPrimary)
            }
        } else {
            Text(
                stringResource(Res.string.app_name),
                style = AppTheme.typography.label,
                color = colors.textPrimary,
            )
        }
    }
}
