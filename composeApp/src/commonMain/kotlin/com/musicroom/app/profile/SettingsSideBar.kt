package com.musicroom.app.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.AudioLines
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.KeyRound
import com.composables.icons.lucide.Link
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.Sparkles
import com.musicroom.app.auth.TermsDialog
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.logout
import com.musicroom.app.generated.resources.my_songs_title
import com.musicroom.app.generated.resources.premium_title
import com.musicroom.app.generated.resources.profile_appearance
import com.musicroom.app.generated.resources.profile_edit
import com.musicroom.app.generated.resources.profile_password
import com.musicroom.app.generated.resources.profile_section_about
import com.musicroom.app.generated.resources.profile_section_account
import com.musicroom.app.generated.resources.profile_section_music
import com.musicroom.app.generated.resources.profile_section_preferences
import com.musicroom.app.generated.resources.profile_section_security
import com.musicroom.app.generated.resources.social_section
import com.musicroom.app.generated.resources.audio_providers_title
import com.musicroom.app.generated.resources.terms_title
import com.musicroom.app.generated.resources.twofa_settings_title
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Second side menu on large screens, next to the main one while the settings are open, with the same
 * look so the two read as one: the settings categories. Clicking one shows its page on the right ([selected]).
 * The user is not repeated here: the main menu already shows them at the top.
 */
@Composable
fun SettingsSideBar(
    selected: ProfileRoute,
    onOpen: (ProfileRoute) -> Unit,
    onLogout: () -> Unit,
) {
    val colors = AppTheme.colors
    val border = AppTheme.spacing.border
    var showTerms by remember { mutableStateOf(false) }

    @Composable
    fun section(title: String) {
        Text(
            title,
            style = AppTheme.typography.bodySmall,
            color = colors.textDisabled,
            modifier = Modifier.padding(start = AppTheme.spacing.md, top = AppTheme.spacing.sm),
        )
    }

    @Composable
    fun page(route: ProfileRoute, icon: ImageVector, label: String) {
        SettingsSideItem(icon, label, selected = route == selected, onClick = { onOpen(route) })
    }

    Column(
        Modifier.width(256.dp).fillMaxHeight().background(colors.surface)
            .drawBehind { drawLine(colors.border, Offset(size.width, 0f), Offset(size.width, size.height), border.toPx()) }
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        section(stringResource(Res.string.profile_section_account))
        page(ProfileRoute.EditProfile, Lucide.Pencil, stringResource(Res.string.profile_edit))
        page(ProfileRoute.SocialLinks, Lucide.Link, stringResource(Res.string.social_section))

        section(stringResource(Res.string.profile_section_music))
        page(ProfileRoute.AudioProviders, Lucide.AudioLines, stringResource(Res.string.audio_providers_title))
        page(ProfileRoute.MySongs, Lucide.Music, stringResource(Res.string.my_songs_title))
        page(ProfileRoute.Premium, Lucide.Sparkles, stringResource(Res.string.premium_title))

        section(stringResource(Res.string.profile_section_security))
        page(ProfileRoute.ChangePassword, Lucide.KeyRound, stringResource(Res.string.profile_password))
        page(ProfileRoute.TwoFactor, Lucide.ShieldCheck, stringResource(Res.string.twofa_settings_title))

        section(stringResource(Res.string.profile_section_preferences))
        page(ProfileRoute.Appearance, Lucide.Palette, stringResource(Res.string.profile_appearance))

        section(stringResource(Res.string.profile_section_about))
        SettingsSideItem(Lucide.FileText, stringResource(Res.string.terms_title), selected = false, onClick = { showTerms = true })
        SettingsSideItem(Lucide.LogOut, stringResource(Res.string.logout), selected = false, onClick = onLogout, destructive = true)
    }

    if (showTerms) TermsDialog(onDismiss = { showTerms = false })
}
