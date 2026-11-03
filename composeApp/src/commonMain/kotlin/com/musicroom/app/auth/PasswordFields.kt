package com.musicroom.app.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.KeyRound
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.password_match
import com.musicroom.app.generated.resources.password_rules
import com.musicroom.app.generated.resources.validation_password_mismatch
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.AuthValidation
import org.jetbrains.compose.resources.stringResource

/** New password + confirmation, with live feedback on the rules and on the match. */
@Composable
fun PasswordFields(
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmation: String,
    onConfirmationChange: (String) -> Unit,
    passwordLabel: String,
    confirmationLabel: String,
    enabled: Boolean,
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    AppTextField(
        password, onPasswordChange, passwordLabel, Modifier.fillMaxWidth(),
        enabled = enabled, isPassword = true, leadingIcon = Lucide.Lock,
    )
    Text(
        stringResource(Res.string.password_rules, AuthValidation.PASSWORD_MIN_LENGTH),
        style = typography.bodySmall,
        color = if (password.isNotEmpty() && AuthValidation.passwordError(password) == null) colors.success else colors.textSecondary,
    )
    AppTextField(
        confirmation, onConfirmationChange, confirmationLabel, Modifier.fillMaxWidth(),
        enabled = enabled, isPassword = true, leadingIcon = Lucide.KeyRound,
    )
    if (confirmation.isNotEmpty()) {
        val matches = confirmation == password
        Text(
            stringResource(if (matches) Res.string.password_match else Res.string.validation_password_mismatch),
            style = typography.bodySmall,
            color = if (matches) colors.success else colors.error,
        )
    }
}
