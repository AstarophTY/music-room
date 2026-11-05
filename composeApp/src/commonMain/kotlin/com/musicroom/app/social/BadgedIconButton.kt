package com.musicroom.app.social

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.musicroom.app.theme.tokens.AppTheme

/** Icon button of the top bar with a [CountBadge] on its top right corner. */
@Composable
fun BadgedIconButton(icon: ImageVector, label: String, count: Int, onClick: () -> Unit) {
    Box {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = AppTheme.colors.textPrimary)
        }
        CountBadge(count, Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 4.dp))
    }
}
