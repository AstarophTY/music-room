package com.musicroom.app.create

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.UserCheck
import com.composables.icons.lucide.Users
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.playlist_license
import com.musicroom.app.generated.resources.playlist_license_everyone
import com.musicroom.app.generated.resources.playlist_license_everyone_body
import com.musicroom.app.generated.resources.playlist_license_invited
import com.musicroom.app.generated.resources.playlist_license_invited_body
import com.musicroom.app.generated.resources.playlist_visibility
import com.musicroom.app.generated.resources.playlist_visibility_private
import com.musicroom.app.generated.resources.playlist_visibility_private_body
import com.musicroom.app.generated.resources.playlist_visibility_public
import com.musicroom.app.generated.resources.playlist_visibility_public_body
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.dto.PlaylistEditLicense
import com.musicroom.shared.dto.PlaylistVisibility
import org.jetbrains.compose.resources.stringResource

/** Who can see a playlist and who can edit it. */
@Composable
fun PlaylistRightsOptions(
    visibility: PlaylistVisibility,
    license: PlaylistEditLicense,
    onVisibility: (PlaylistVisibility) -> Unit,
    onLicense: (PlaylistEditLicense) -> Unit,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    Text(stringResource(Res.string.playlist_visibility), style = AppTheme.typography.label, color = colors.textSecondary)
    PlaylistChoiceOption(
        Lucide.Globe,
        stringResource(Res.string.playlist_visibility_public),
        stringResource(Res.string.playlist_visibility_public_body),
        selected = visibility == PlaylistVisibility.PUBLIC,
        onClick = { onVisibility(PlaylistVisibility.PUBLIC) },
        enabled = enabled,
    )
    PlaylistChoiceOption(
        Lucide.Lock,
        stringResource(Res.string.playlist_visibility_private),
        stringResource(Res.string.playlist_visibility_private_body),
        selected = visibility == PlaylistVisibility.PRIVATE,
        onClick = { onVisibility(PlaylistVisibility.PRIVATE) },
        enabled = enabled,
    )
    Text(stringResource(Res.string.playlist_license), style = AppTheme.typography.label, color = colors.textSecondary)
    PlaylistChoiceOption(
        Lucide.Users,
        stringResource(Res.string.playlist_license_everyone),
        stringResource(Res.string.playlist_license_everyone_body),
        selected = license == PlaylistEditLicense.EVERYONE,
        onClick = { onLicense(PlaylistEditLicense.EVERYONE) },
        enabled = enabled,
    )
    PlaylistChoiceOption(
        Lucide.UserCheck,
        stringResource(Res.string.playlist_license_invited),
        stringResource(Res.string.playlist_license_invited_body),
        selected = license == PlaylistEditLicense.INVITED,
        onClick = { onLicense(PlaylistEditLicense.INVITED) },
        enabled = enabled,
    )
}
