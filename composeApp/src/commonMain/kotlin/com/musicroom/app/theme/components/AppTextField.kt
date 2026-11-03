package com.musicroom.app.theme.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Lucide
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme

/** Text field of the design system. Password fields get a show/hide button. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: ImageVector? = null,
) {
    val colors = AppTheme.colors
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = modifier, enabled = enabled,
        label = { Text(label, style = AppTheme.typography.label) }, singleLine = true,
        textStyle = AppTheme.typography.body, shape = AppShapes.medium,
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(if (visible) Lucide.EyeOff else Lucide.Eye, contentDescription = null)
                }
            }
        } else {
            null
        },
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
            disabledTextColor = colors.textDisabled, focusedBorderColor = colors.accentBorder,
            unfocusedBorderColor = colors.border, disabledBorderColor = colors.border,
            focusedLabelColor = colors.accentText, unfocusedLabelColor = colors.textSecondary,
            cursorColor = colors.accentText, focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface, disabledContainerColor = colors.surfaceSubtle,
            focusedLeadingIconColor = colors.accentText, unfocusedLeadingIconColor = colors.textSecondary,
            focusedTrailingIconColor = colors.textSecondary, unfocusedTrailingIconColor = colors.textSecondary,
        ),
    )
}
