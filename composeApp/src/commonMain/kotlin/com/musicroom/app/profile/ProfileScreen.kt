package com.musicroom.app.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.AudioLines
import com.composables.icons.lucide.BadgeCheck
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.KeyRound
import com.composables.icons.lucide.Languages
import com.composables.icons.lucide.Link
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.ShieldCheck
import com.musicroom.app.auth.TermsDialog
import com.musicroom.app.common.UserAvatar
import com.musicroom.app.common.icon
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.*
import com.musicroom.app.theme.ThemeMode
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.common.deviceName
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UserResponse
import org.jetbrains.compose.resources.stringResource

/** Profile header and settings menu, opened from the avatar in the top bar. */
@Composable
fun ProfileScreen(
    api: MusicRoomApi,
    user: UserResponse,
    themeMode: ThemeMode,
    onBack: () -> Unit,
    onOpen: (ProfileRoute) -> Unit,
    onLogout: () -> Unit,
) {
    val colors = AppTheme.colors
    var showTerms by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val themeLabel = stringResource(
        when (themeMode) {
            ThemeMode.System -> Res.string.appearance_system
            ThemeMode.Light -> Res.string.appearance_light
            ThemeMode.Dark -> Res.string.appearance_dark
        },
    )

    SubScreenScaffold(title = stringResource(Res.string.profile_title), onBack = onBack) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            UserAvatar(api, user, size = 96.dp)
            Text(user.displayName, style = AppTheme.typography.title, color = colors.textPrimary)
            Text(user.email, style = AppTheme.typography.body, color = colors.textSecondary)
            if (user.emailVerified) {
                Row(
                    Modifier.background(colors.accentSubtle, AppShapes.pill)
                        .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Lucide.BadgeCheck, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(16.dp))
                    Text(stringResource(Res.string.profile_verified), style = AppTheme.typography.bodySmall, color = colors.accentText)
                }
            }
        }

        Text(stringResource(Res.string.profile_section_account), style = AppTheme.typography.label, color = colors.textSecondary)
        AppCard(Modifier.fillMaxWidth()) {
            SettingsRow(Lucide.Pencil, stringResource(Res.string.profile_edit), { onOpen(ProfileRoute.EditProfile) })
        }

        Text(stringResource(Res.string.social_section), style = AppTheme.typography.label, color = colors.textSecondary)
        AppCard(Modifier.fillMaxWidth()) {
            user.socialLinks.forEach { link ->
                val url = link.network.urlFor(link.handle)
                SettingsRow(
                    link.network.icon(),
                    link.network.displayName,
                    onClick = { if (url != null) runCatching { uriHandler.openUri(url) } },
                    value = "@${link.handle}",
                    showChevron = url != null,
                )
            }
            SettingsRow(
                Lucide.Link,
                stringResource(if (user.socialLinks.isEmpty()) Res.string.social_link_cta else Res.string.social_edit),
                { onOpen(ProfileRoute.SocialLinks) },
            )
        }

        Text(stringResource(Res.string.profile_section_music), style = AppTheme.typography.label, color = colors.textSecondary)
        AppCard(Modifier.fillMaxWidth()) {
            SettingsRow(
                Lucide.AudioLines,
                stringResource(Res.string.spotify_title),
                { onOpen(ProfileRoute.Spotify) },
                value = stringResource(if (user.spotifyConnected) Res.string.spotify_connected else Res.string.spotify_not_connected),
            )
        }

        Text(stringResource(Res.string.profile_section_security), style = AppTheme.typography.label, color = colors.textSecondary)
        AppCard(Modifier.fillMaxWidth()) {
            SettingsRow(Lucide.KeyRound, stringResource(Res.string.profile_password), { onOpen(ProfileRoute.ChangePassword) })
            SettingsRow(
                Lucide.ShieldCheck,
                stringResource(Res.string.twofa_settings_title),
                { onOpen(ProfileRoute.TwoFactor) },
                value = stringResource(if (user.twoFactorEnabled) Res.string.twofa_status_on else Res.string.twofa_status_off),
            )
        }

        Text(stringResource(Res.string.profile_section_preferences), style = AppTheme.typography.label, color = colors.textSecondary)
        AppCard(Modifier.fillMaxWidth()) {
            SettingsRow(
                Lucide.Palette,
                stringResource(Res.string.profile_appearance),
                { onOpen(ProfileRoute.Appearance) },
                value = themeLabel,
            )
            SettingsRow(
                Lucide.Languages,
                stringResource(Res.string.profile_language),
                onClick = {},
                value = stringResource(Res.string.profile_language_value, stringResource(Res.string.language_name), deviceName()),
                showChevron = false,
            )
        }

        Text(stringResource(Res.string.profile_section_about), style = AppTheme.typography.label, color = colors.textSecondary)
        AppCard(Modifier.fillMaxWidth()) {
            SettingsRow(Lucide.FileText, stringResource(Res.string.terms_title), { showTerms = true })
            SettingsRow(Lucide.LogOut, stringResource(Res.string.logout), onLogout, destructive = true, showChevron = false)
        }
    }

    if (showTerms) TermsDialog(onDismiss = { showTerms = false })
}
