package com.musicroom.app.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.composables.icons.lucide.AudioLines
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.KeyRound
import com.composables.icons.lucide.Languages
import com.composables.icons.lucide.Link
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Users
import com.musicroom.app.auth.TermsDialog
import com.musicroom.app.common.Reveal
import com.musicroom.app.common.TabContent
import com.musicroom.app.common.WindowSize
import com.musicroom.app.common.deviceName
import com.musicroom.app.common.icon
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.appearance_dark
import com.musicroom.app.generated.resources.appearance_light
import com.musicroom.app.generated.resources.appearance_system
import com.musicroom.app.generated.resources.audio_providers_title
import com.musicroom.app.generated.resources.friends_pending
import com.musicroom.app.generated.resources.friends_title
import com.musicroom.app.generated.resources.language_name
import com.musicroom.app.generated.resources.logout
import com.musicroom.app.generated.resources.my_songs_title
import com.musicroom.app.generated.resources.premium_title
import com.musicroom.app.generated.resources.profile_appearance
import com.musicroom.app.generated.resources.profile_details_title
import com.musicroom.app.generated.resources.profile_edit
import com.musicroom.app.generated.resources.profile_language
import com.musicroom.app.generated.resources.profile_language_value
import com.musicroom.app.generated.resources.profile_password
import com.musicroom.app.generated.resources.profile_section_about
import com.musicroom.app.generated.resources.profile_section_account
import com.musicroom.app.generated.resources.profile_section_music
import com.musicroom.app.generated.resources.profile_section_preferences
import com.musicroom.app.generated.resources.profile_section_security
import com.musicroom.app.generated.resources.social_edit
import com.musicroom.app.generated.resources.social_link_cta
import com.musicroom.app.generated.resources.social_section
import com.musicroom.app.generated.resources.tab_settings
import com.musicroom.app.generated.resources.terms_title
import com.musicroom.app.generated.resources.twofa_settings_title
import com.musicroom.app.generated.resources.twofa_status_off
import com.musicroom.app.generated.resources.twofa_status_on
import com.musicroom.app.theme.ThemeMode
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UserResponse
import org.jetbrains.compose.resources.stringResource

/**
 * Settings tab: the account header, then the settings grouped by theme. Each row opens its page, which
 * replaces the tab; the player keeps running under it.
 */
@Composable
fun SettingsScreen(
    api: MusicRoomApi,
    user: UserResponse,
    themeMode: ThemeMode,
    pendingRequests: Int,
    onOpen: (ProfileRoute) -> Unit,
    onOpenFriends: () -> Unit,
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

    TabContent {
        if (WindowSize.current != WindowSize.Compact) {
            Text(stringResource(Res.string.tab_settings), style = AppTheme.typography.display, color = colors.textPrimary)
        }
        ProfileHeader(api, user)

        Reveal(3) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                Text(stringResource(Res.string.profile_section_account), style = AppTheme.typography.label, color = colors.textSecondary)
                AppCard(Modifier.fillMaxWidth()) {
                    SettingsRow(Lucide.Pencil, stringResource(Res.string.profile_edit), { onOpen(ProfileRoute.EditProfile) })
                    SettingsRow(Lucide.FileText, stringResource(Res.string.profile_details_title), { onOpen(ProfileRoute.ProfileDetails) })
                    SettingsRow(
                        Lucide.Users,
                        stringResource(Res.string.friends_title),
                        onOpenFriends,
                        value = if (pendingRequests > 0) stringResource(Res.string.friends_pending, pendingRequests) else null,
                    )
                }
            }
        }

        Reveal(4) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
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
            }
        }

        Reveal(5) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                Text(stringResource(Res.string.profile_section_music), style = AppTheme.typography.label, color = colors.textSecondary)
                AppCard(Modifier.fillMaxWidth()) {
                    SettingsRow(
                        Lucide.AudioLines,
                        stringResource(Res.string.audio_providers_title),
                        { onOpen(ProfileRoute.AudioProviders) },
                    )
                    SettingsRow(Lucide.Music, stringResource(Res.string.my_songs_title), { onOpen(ProfileRoute.MySongs) })
                    SettingsRow(Lucide.Sparkles, stringResource(Res.string.premium_title), { onOpen(ProfileRoute.Premium) })
                }
            }
        }

        Reveal(6) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
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
            }
        }

        Reveal(7) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
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
            }
        }

        Reveal(8) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                Text(stringResource(Res.string.profile_section_about), style = AppTheme.typography.label, color = colors.textSecondary)
                AppCard(Modifier.fillMaxWidth()) {
                    SettingsRow(Lucide.FileText, stringResource(Res.string.terms_title), { showTerms = true })
                    SettingsRow(Lucide.LogOut, stringResource(Res.string.logout), onLogout, destructive = true, showChevron = false)
                }
            }
        }
    }

    if (showTerms) TermsDialog(onDismiss = { showTerms = false })
}
