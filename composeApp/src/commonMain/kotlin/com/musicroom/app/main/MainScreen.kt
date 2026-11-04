package com.musicroom.app.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.musicroom.app.common.WindowSize
import com.musicroom.app.create.CreateScreen
import com.musicroom.app.events.EventsScreen
import com.musicroom.app.home.HomeScreen
import com.musicroom.app.playlists.PlaylistsScreen
import com.musicroom.app.profile.AppearanceScreen
import com.musicroom.app.profile.ChangePasswordScreen
import com.musicroom.app.profile.EditProfileScreen
import com.musicroom.app.profile.ProfileRoute
import com.musicroom.app.profile.ProfileScreen
import com.musicroom.app.profile.SocialLinksScreen
import com.musicroom.app.profile.TwoFactorSettingsScreen
import com.musicroom.app.search.SearchScreen
import com.musicroom.app.theme.ThemeMode
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.launch

/** Logged-in part of the app: top bar, current tab, bottom bar, and the profile pages on top. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(
    api: MusicRoomApi,
    user: UserResponse,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onUserChange: (UserResponse) -> Unit,
    onLoggedOut: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var profileRoute by rememberSaveable { mutableStateOf<ProfileRoute?>(null) }
    val scope = rememberCoroutineScope()

    // Android back: close the profile pages first, then go back to the home tab.
    BackHandler(enabled = profileRoute != null || tab != MainTab.Home) {
        val route = profileRoute
        if (route != null) profileRoute = route.parent else tab = MainTab.Home
    }

    val openProfile = { profileRoute = ProfileRoute.Profile }
    val content: @Composable () -> Unit = {
        when (tab) {
            MainTab.Home -> HomeScreen(
                user = user,
                onCreate = { tab = MainTab.Create },
                onOpenPlaylists = { tab = MainTab.Playlists },
            )
            MainTab.Search -> SearchScreen()
            MainTab.Events -> EventsScreen(onCreate = { tab = MainTab.Create })
            MainTab.Playlists -> PlaylistsScreen(onCreate = { tab = MainTab.Create })
            MainTab.Create -> CreateScreen()
        }
    }

    val back = { profileRoute = profileRoute?.parent }
    when (profileRoute) {
        ProfileRoute.Profile -> return ProfileScreen(
            api = api,
            user = user,
            serverUrl = api.baseUrl,
            themeMode = themeMode,
            onBack = back,
            onOpen = { profileRoute = it },
            onLogout = {
                scope.launch {
                    api.logout()
                    onLoggedOut()
                }
            },
        )
        ProfileRoute.EditProfile -> return EditProfileScreen(api, user, onUserChange, onBack = back)
        ProfileRoute.SocialLinks -> return SocialLinksScreen(api, user, onUserChange, onBack = back)
        ProfileRoute.ChangePassword -> return ChangePasswordScreen(api, onBack = back)
        ProfileRoute.TwoFactor -> return TwoFactorSettingsScreen(api, user, onUserChange, onBack = back)
        ProfileRoute.Appearance -> return AppearanceScreen(themeMode, onThemeModeChange, onBack = back)
        // Profile pages replace the tabs (instead of covering them) so touches cannot reach the tabs below.
        null -> Unit
    }

    if (WindowSize.current == WindowSize.Expanded) {
        // Large screen: side menu on the left instead of the top and bottom bars.
        Row(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
            SideNavBar(api, user, selected = tab, onSelect = { tab = it }, onOpenProfile = openProfile)
            Box(Modifier.weight(1f).fillMaxHeight().safeDrawingPadding()) { content() }
        }
    } else {
        Column(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
            MainTopBar(api, user, onOpenProfile = openProfile)
            Box(Modifier.weight(1f).fillMaxWidth().imePadding()) { content() }
            BottomNavBar(selected = tab, onSelect = { tab = it })
        }
    }
}
