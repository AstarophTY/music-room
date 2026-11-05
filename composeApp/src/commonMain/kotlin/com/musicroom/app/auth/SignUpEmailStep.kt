package com.musicroom.app.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mail
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.field_email
import com.musicroom.app.theme.components.AppTextField
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignUpEmailStep(email: String, onEmailChange: (String) -> Unit, enabled: Boolean) {
    AppTextField(
        email, onEmailChange, stringResource(Res.string.field_email), Modifier.fillMaxWidth(),
        enabled = enabled, keyboardType = KeyboardType.Email, leadingIcon = Lucide.Mail,
    )
}
