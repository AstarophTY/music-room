package com.musicroom.app.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ShieldCheck
import com.musicroom.app.common.StatusMessage
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.*
import com.musicroom.app.theme.color.LightColors
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TwoFactorCodeRequest
import com.musicroom.shared.dto.TwoFactorSetupResponse
import com.musicroom.shared.dto.UserResponse
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Turns two-factor authentication on (QR code -> first code -> recovery codes) or off (needs a code).
 */
@Composable
fun TwoFactorSettingsScreen(api: MusicRoomApi, user: UserResponse, onUserChange: (UserResponse) -> Unit, onBack: () -> Unit) {
    var setup by remember { mutableStateOf<TwoFactorSetupResponse?>(null) }
    var recoveryCodes by remember { mutableStateOf<List<String>?>(null) }
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<StatusMessage?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    fun run(action: suspend () -> Unit) {
        busy = true
        status = null
        scope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status = StatusMessage(UiText.of(e), isError = true)
            } finally {
                busy = false
            }
        }
    }

    @Composable
    fun CodeInput() = AppTextField(
        code,
        { code = if (user.twoFactorEnabled) it.uppercase().take(11) else it.filter(Char::isDigit).take(6) },
        stringResource(Res.string.twofa_field_code),
        Modifier.fillMaxWidth(),
        enabled = !busy,
        keyboardType = if (user.twoFactorEnabled) KeyboardType.Text else KeyboardType.NumberPassword,
        leadingIcon = Lucide.ShieldCheck,
    )

    SubScreenScaffold(title = stringResource(Res.string.twofa_settings_title), onBack = onBack) {
        val codes = recoveryCodes
        val pending = setup
        when {
            codes != null -> {
                Text(stringResource(Res.string.twofa_recovery_title), style = typography.title, color = colors.textPrimary)
                Text(stringResource(Res.string.twofa_recovery_body), style = typography.body, color = colors.textSecondary)
                AppCard(Modifier.fillMaxWidth()) {
                    codes.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth().padding(vertical = AppTheme.spacing.xs)) {
                            pair.forEach {
                                Text(
                                    it,
                                    modifier = Modifier.weight(1f),
                                    style = typography.body.copy(fontFamily = FontFamily.Monospace),
                                    color = colors.textPrimary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
                AppButton(
                    stringResource(Res.string.twofa_copy_codes),
                    {
                        clipboard.setText(AnnotatedString(codes.joinToString("\n")))
                        status = StatusMessage(UiText.of(Res.string.common_copied), isError = false)
                    },
                    Modifier.fillMaxWidth(),
                    variant = AppButtonVariant.Secondary,
                    icon = Lucide.Copy,
                )
                AppButton(stringResource(Res.string.twofa_done), onBack, Modifier.fillMaxWidth())
            }

            user.twoFactorEnabled -> {
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.ShieldCheck, contentDescription = null, tint = colors.success, modifier = Modifier.size(28.dp))
                    Text(stringResource(Res.string.twofa_status_on), style = typography.title, color = colors.textPrimary)
                }
                Text(stringResource(Res.string.twofa_enabled_body), style = typography.body, color = colors.textSecondary)
                Text(stringResource(Res.string.twofa_disable_hint), style = typography.bodySmall, color = colors.textSecondary)
                CodeInput()
                AppButton(
                    stringResource(if (busy) Res.string.common_please_wait else Res.string.twofa_disable),
                    {
                        run {
                            onUserChange(api.disableTwoFactor(TwoFactorCodeRequest(code)))
                            code = ""
                            status = StatusMessage(UiText.of(Res.string.twofa_disabled_done), isError = false)
                        }
                    },
                    Modifier.fillMaxWidth(),
                    enabled = !busy && code.isNotBlank(),
                    variant = AppButtonVariant.Secondary,
                )
            }

            pending != null -> {
                Text(stringResource(Res.string.twofa_scan), style = typography.heading, color = colors.textPrimary)
                // Always light behind the QR code: scanners need dark modules on a light background.
                Box(
                    Modifier.align(Alignment.CenterHorizontally).background(LightColors.surface, AppShapes.large)
                        .padding(AppTheme.spacing.md),
                ) {
                    Image(rememberQrCodePainter(pending.otpauthUri), contentDescription = null, modifier = Modifier.size(200.dp))
                }
                Text(stringResource(Res.string.twofa_or_key), style = typography.bodySmall, color = colors.textSecondary)
                Text(
                    pending.secret.chunked(4).joinToString(" "),
                    modifier = Modifier.fillMaxWidth().background(colors.surfaceSubtle, AppShapes.medium).padding(AppTheme.spacing.md),
                    style = typography.body.copy(fontFamily = FontFamily.Monospace),
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    AppButton(
                        stringResource(Res.string.twofa_copy_key),
                        {
                            clipboard.setText(AnnotatedString(pending.secret))
                            status = StatusMessage(UiText.of(Res.string.common_copied), isError = false)
                        },
                        variant = AppButtonVariant.Secondary,
                        icon = Lucide.Copy,
                    )
                    AppButton(
                        stringResource(Res.string.twofa_open_app),
                        { runCatching { uriHandler.openUri(pending.otpauthUri) } },
                        variant = AppButtonVariant.Ghost,
                        icon = Lucide.ExternalLink,
                    )
                }
                Text(stringResource(Res.string.twofa_confirm_step), style = typography.heading, color = colors.textPrimary)
                CodeInput()
                AppButton(
                    stringResource(if (busy) Res.string.common_please_wait else Res.string.twofa_confirm),
                    {
                        run {
                            val result = api.enableTwoFactor(TwoFactorCodeRequest(code))
                            onUserChange(result.user)
                            recoveryCodes = result.recoveryCodes
                            code = ""
                        }
                    },
                    Modifier.fillMaxWidth(),
                    enabled = !busy && code.length == 6,
                )
            }

            else -> {
                Text(stringResource(Res.string.twofa_status_off), style = typography.title, color = colors.textPrimary)
                Text(stringResource(Res.string.twofa_intro), style = typography.body, color = colors.textSecondary)
                AppButton(
                    stringResource(if (busy) Res.string.common_please_wait else Res.string.twofa_enable),
                    { run { setup = api.setupTwoFactor() } },
                    Modifier.fillMaxWidth(),
                    enabled = !busy,
                    icon = Lucide.ShieldCheck,
                )
            }
        }
        status?.let { Text(it.text.resolve(), style = typography.bodySmall, color = if (it.isError) colors.error else colors.success) }
    }
}
