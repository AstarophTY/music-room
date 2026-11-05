package com.musicroom.app.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.*
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Shows the terms of use. With [onAccept], the user can accept them from the dialog; without it, read only.
 * The text is a draft: have it reviewed before a public release.
 */
@Composable
fun TermsDialog(onDismiss: () -> Unit, onAccept: (() -> Unit)? = null) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val sections = listOf(
        Res.string.terms_1_title to Res.string.terms_1_body,
        Res.string.terms_2_title to Res.string.terms_2_body,
        Res.string.terms_3_title to Res.string.terms_3_body,
        Res.string.terms_4_title to Res.string.terms_4_body,
        Res.string.terms_5_title to Res.string.terms_5_body,
        Res.string.terms_6_title to Res.string.terms_6_body,
        Res.string.terms_7_title to Res.string.terms_7_body,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = AppShapes.large,
        title = { Text(stringResource(Res.string.terms_title), style = typography.title, color = colors.textPrimary) },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                sections.forEach { (heading, body) ->
                    Text(stringResource(heading), style = typography.heading, color = colors.textPrimary)
                    Text(stringResource(body), style = typography.bodySmall, color = colors.textSecondary)
                }
            }
        },
        confirmButton = {
            if (onAccept != null) {
                AppButton(text = stringResource(Res.string.terms_accept), onClick = onAccept)
            } else {
                AppButton(text = stringResource(Res.string.common_close), onClick = onDismiss, variant = AppButtonVariant.Secondary)
            }
        },
        dismissButton = if (onAccept != null) {
            { AppButton(text = stringResource(Res.string.common_cancel), onClick = onDismiss, variant = AppButtonVariant.Ghost) }
        } else {
            null
        },
    )
}
