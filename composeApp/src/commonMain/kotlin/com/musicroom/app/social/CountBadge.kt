package com.musicroom.app.social

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme

/** Small amber pill with a number ("9+" above nine); pops in and out as [count] becomes 0 or more. */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    AnimatedVisibility(count > 0, modifier, enter = scaleIn(), exit = scaleOut()) {
        Box(
            Modifier.defaultMinSize(minWidth = 18.dp, minHeight = 18.dp).background(colors.accent, AppShapes.pill)
                .padding(horizontal = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (count > 9) "9+" else count.toString(),
                style = AppTheme.typography.label.copy(fontSize = 11.sp, lineHeight = 14.sp),
                color = colors.onAccent,
            )
        }
    }
}
