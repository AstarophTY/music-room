package com.musicroom.app.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.musicroom.app.common.Avatar
import com.musicroom.app.common.Equalizer
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.app_name
import com.musicroom.app.generated.resources.profile_open
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.dto.UserResponse
import org.jetbrains.compose.resources.stringResource

/** Avatar on the left (opens the profile), app name and a small live equalizer. */
@Composable
fun MainTopBar(user: UserResponse, onOpenProfile: () -> Unit) {
    val colors = AppTheme.colors
    val openProfile = stringResource(Res.string.profile_open)
    Row(
        Modifier.fillMaxWidth().statusBarsPadding()
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            user.displayName,
            Modifier.pressable(onClick = onOpenProfile).semantics { contentDescription = openProfile },
            size = 40.dp,
        )
        Text(
            stringResource(Res.string.app_name),
            modifier = Modifier.weight(1f),
            style = AppTheme.typography.heading,
            color = colors.textPrimary,
        )
        Equalizer(color = colors.accentText, height = 18.dp)
    }
}
