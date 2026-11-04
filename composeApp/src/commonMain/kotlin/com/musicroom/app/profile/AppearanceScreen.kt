package com.musicroom.app.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.Sun
import com.composables.icons.lucide.SunMoon
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.appearance_dark
import com.musicroom.app.generated.resources.appearance_hint
import com.musicroom.app.generated.resources.appearance_light
import com.musicroom.app.generated.resources.appearance_system
import com.musicroom.app.generated.resources.appearance_system_hint
import com.musicroom.app.generated.resources.profile_appearance
import com.musicroom.app.theme.ThemeMode
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.common.deviceName
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Light, dark, or follow the device. The choice is saved on the device. */
@Composable
fun AppearanceScreen(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit, onBack: () -> Unit) {
    SubScreenScaffold(title = stringResource(Res.string.profile_appearance), onBack = onBack) {
        Text(stringResource(Res.string.appearance_hint), style = AppTheme.typography.body, color = AppTheme.colors.textSecondary)
        AppCard(Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEach { mode ->
                SettingsRow(
                    icon = when (mode) {
                        ThemeMode.System -> Lucide.SunMoon
                        ThemeMode.Light -> Lucide.Sun
                        ThemeMode.Dark -> Lucide.Moon
                    },
                    label = stringResource(
                        when (mode) {
                            ThemeMode.System -> Res.string.appearance_system
                            ThemeMode.Light -> Res.string.appearance_light
                            ThemeMode.Dark -> Res.string.appearance_dark
                        },
                    ),
                    value = if (mode == ThemeMode.System) stringResource(Res.string.appearance_system_hint, deviceName()) else null,
                    onClick = { onThemeModeChange(mode) },
                    showChevron = false,
                    selected = mode == themeMode,
                )
            }
        }
    }
}
