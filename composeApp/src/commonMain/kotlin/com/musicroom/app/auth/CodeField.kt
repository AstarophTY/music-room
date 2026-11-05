package com.musicroom.app.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MailCheck
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.field_code
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.shared.AuthValidation
import org.jetbrains.compose.resources.stringResource

/** Field for the 6-digit code received by email: digits only, length capped. */
@Composable
fun CodeField(code: String, onCodeChange: (String) -> Unit, enabled: Boolean) {
    AppTextField(
        value = code,
        onValueChange = { input -> onCodeChange(input.filter { it.isDigit() }.take(AuthValidation.VERIFICATION_CODE_LENGTH)) },
        label = stringResource(Res.string.field_code),
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        keyboardType = KeyboardType.NumberPassword,
        leadingIcon = Lucide.MailCheck,
    )
}
