package com.musicroom.app.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.dto.AvatarColor

/** Row of round swatches, one per avatar color. */
@Composable
fun AvatarColorPicker(selected: AvatarColor, onSelect: (AvatarColor) -> Unit, enabled: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        AvatarColor.entries.forEach { color ->
            val tint = AppTheme.colors.avatars.getValue(color)
            Box(
                Modifier.size(40.dp)
                    .pressable(enabled) { onSelect(color) }
                    .background(tint.background, CircleShape)
                    .border(if (color == selected) 3.dp else AppTheme.spacing.border, if (color == selected) tint.text else tint.border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (color == selected) Icon(Lucide.Check, contentDescription = null, tint = tint.text, modifier = Modifier.size(18.dp))
            }
        }
    }
}
