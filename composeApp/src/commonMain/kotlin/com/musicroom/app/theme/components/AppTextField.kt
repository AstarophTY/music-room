package com.musicroom.app.theme.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme

@Composable
fun AppTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = AppTheme.colors
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = modifier, enabled = enabled,
        label = { Text(label, style = AppTheme.typography.label) }, singleLine = true,
        textStyle = AppTheme.typography.body, shape = AppShapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
            disabledTextColor = colors.textDisabled, focusedBorderColor = colors.borderStrong,
            unfocusedBorderColor = colors.border, disabledBorderColor = colors.border,
            focusedLabelColor = colors.textSecondary, unfocusedLabelColor = colors.textSecondary,
            cursorColor = colors.accent, focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface, disabledContainerColor = colors.surfaceSubtle,
        ),
    )
}
