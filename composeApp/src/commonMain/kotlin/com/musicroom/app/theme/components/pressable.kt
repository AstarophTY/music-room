package com.musicroom.app.theme.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role

@Composable
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    return this.alpha(if (pressed) 0.7f else 1f).clickable(
        interactionSource = interaction, indication = null, enabled = enabled,
        role = Role.Button, onClick = onClick,
    )
}
