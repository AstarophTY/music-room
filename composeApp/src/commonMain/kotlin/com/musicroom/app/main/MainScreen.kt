package com.musicroom.app.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.musicroom.app.common.WindowSize
import com.musicroom.app.create.CreateSheet
import com.musicroom.app.events.EventsScreen
import com.musicroom.app.home.HomeScreen
import com.musicroom.app.player.PlayerBar
import com.musicroom.app.player.PlayerController
import com.musicroom.app.playlists.PlaylistsScreen
import com.musicroom.app.profile.AppearanceScreen
import com.musicroom.app.profile.ChangePasswordScreen
import com.musicroom.app.profile.EditProfileScreen
import com.musicroom.app.profile.ProfileRoute
import com.musicroom.app.profile.ProfileScreen
import com.musicroom.app.profile.SocialLinksScreen
import com.musicroom.app.profile.SpotifySettingsScreen
import com.musicroom.app.profile.TwoFactorSettingsScreen
import com.musicroom.app.search.SearchScreen
import com.musicroom.app.theme.ThemeMode
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.launch

/** Logged-in part of the app: top bar, current tab, bottom bar, and the profile pages (next to the side menu on large screens). */
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
    var sideNavCollapsed by rememberSaveable { mutableStateOf(false) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var openRoomPlaylist by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val player = remember(api) { PlayerController(api, scope) }
    // Follows what Spotify plays while an account is linked.
    LaunchedEffect(user.spotifyConnected) { if (user.spotifyConnected) player.sync() }
    val onSpotifyConnected = { onUserChange(user.copy(spotifyConnected = true)) }
    val playerBar: @Composable () -> Unit = {
        AnimatedVisibility(
            visible = user.spotifyConnected && (player.track != null || player.error != null),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            PlayerBar(api, player)
        }
    }
    val openCreate = { showCreate = true }
    // The Create tab is not a page: it opens the create card from the bottom.
    val selectTab: (MainTab) -> Unit = { if (it == MainTab.Create) openCreate() else tab = it }

    // Android back: close the profile pages first, then go back to the home tab.
    BackHandler(enabled = profileRoute != null || tab != MainTab.Home) {
        val route = profileRoute
        if (route != null) profileRoute = route.parent else tab = MainTab.Home
    }

    val openProfile = { profileRoute = ProfileRoute.Profile }
    val tabPage: @Composable (MainTab) -> Unit = { shown ->
        when (shown) {
            MainTab.Home -> HomeScreen(
                api = api,
                user = user,
                player = player,
                onSpotifyConnected = onSpotifyConnected,
                onCreate = openCreate,
            )
            MainTab.Search -> SearchScreen(api, user, player, onSpotifyConnected)
            MainTab.Events -> EventsScreen(onCreate = openCreate)
            MainTab.Playlists -> PlaylistsScreen(
                api,
                user,
                player,
                onSpotifyConnected,
                onCreate = openCreate,
                openRoomPlaylistId = openRoomPlaylist,
                onOpenRoomPlaylist = { openRoomPlaylist = it },
            )
            MainTab.Create -> Unit
        }
    }
    // Changing tab fades the new one in while it rises a little.
    val content: @Composable () -> Unit = {
        AnimatedContent(
            tab,
            transitionSpec = {
                (fadeIn(tween(220, delayMillis = 60)) + slideInVertically(tween(260)) { it / 30 })
                    .togetherWith(fadeOut(tween(120)))
            },
        ) { tabPage(it) }
    }

    val back = { profileRoute = profileRoute?.parent }
    val profilePage: @Composable (ProfileRoute) -> Unit = { route ->
        when (route) {
            ProfileRoute.Profile -> ProfileScreen(
                api = api,
                user = user,
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
            ProfileRoute.EditProfile -> EditProfileScreen(api, user, onUserChange, onBack = back)
            ProfileRoute.SocialLinks -> SocialLinksScreen(api, user, onUserChange, onBack = back)
            ProfileRoute.ChangePassword -> ChangePasswordScreen(api, onBack = back)
            ProfileRoute.TwoFactor -> TwoFactorSettingsScreen(api, user, onUserChange, onBack = back)
            ProfileRoute.Appearance -> AppearanceScreen(themeMode, onThemeModeChange, onBack = back)
            ProfileRoute.Spotify -> SpotifySettingsScreen(api, user, onUserChange, onBack = back)
        }
    }

    val route = profileRoute
    if (WindowSize.current == WindowSize.Expanded) {
        // Large screen: side menu on the left instead of the top and bottom bars. It stays visible on the
        // profile pages, which open on the right in place of the tab.
        Row(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
            SideNavBar(
                api,
                user,
                selected = if (route == null) tab else null,
                collapsed = sideNavCollapsed,
                onToggleCollapsed = { sideNavCollapsed = !sideNavCollapsed },
                onSelect = {
                    profileRoute = null
                    selectTab(it)
                },
                onOpenProfile = openProfile,
            )
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (route != null) profilePage(route) else Box(Modifier.fillMaxSize().safeDrawingPadding()) { content() }
                }
                playerBar()
            }
        }
    } else if (route != null) {
        // Profile pages replace the tabs (instead of covering them) so touches cannot reach the tabs below.
        profilePage(route)
    } else {
        Column(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
            MainTopBar(api, user, onOpenProfile = openProfile)
            Box(Modifier.weight(1f).fillMaxWidth().imePadding()) { content() }
            playerBar()
            BottomNavBar(selected = tab, onSelect = selectTab)
        }
    }

    if (showCreate) {
        CreateSheet(
            api,
            onDismiss = { showCreate = false },
            onPlaylistCreated = { playlist ->
                showCreate = false
                profileRoute = null
                openRoomPlaylist = playlist.id
                tab = MainTab.Playlists
            },
        )
    }
}
