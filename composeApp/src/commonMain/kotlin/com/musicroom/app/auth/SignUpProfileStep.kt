package com.musicroom.app.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.field_display_name
import com.musicroom.app.generated.resources.terms_accept_prefix
import com.musicroom.app.generated.resources.terms_link
import com.musicroom.app.theme.components.AppCheckbox
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignUpProfileStep(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    acceptedTerms: Boolean,
    onAcceptedTermsChange: (Boolean) -> Unit,
    onShowTerms: () -> Unit,
    enabled: Boolean,
) {
    val colors = AppTheme.colors
    val prefix = stringResource(Res.string.terms_accept_prefix)
    val link = stringResource(Res.string.terms_link)
    AppTextField(
        displayName, onDisplayNameChange, stringResource(Res.string.field_display_name), Modifier.fillMaxWidth(),
        enabled = enabled, leadingIcon = Lucide.User,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        AppCheckbox(acceptedTerms, onAcceptedTermsChange, enabled = enabled)
        Text(
            buildAnnotatedString {
                append(prefix)
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "terms",
                        styles = TextLinkStyles(
                            SpanStyle(
                                color = colors.accentText,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ),
                    ) { onShowTerms() },
                ) { append(link) }
            },
            style = AppTheme.typography.body,
            color = colors.textSecondary,
        )
    }
}
