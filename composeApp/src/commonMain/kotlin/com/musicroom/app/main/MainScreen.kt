package com.musicroom.app.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.musicroom.app.common.WindowSize
import com.musicroom.app.create.CreateSheet
import com.musicroom.app.events.EventsScreen
import com.musicroom.app.home.HomeScreen
import com.musicroom.app.player.NowPlayingScreen
import com.musicroom.app.player.PlayerBar
import com.musicroom.app.player.PlayerController
import com.musicroom.app.player.Proposer
import com.musicroom.app.player.QueueToast
import com.musicroom.app.playlists.PlaylistsScreen
import com.musicroom.app.profile.AppearanceScreen
import com.musicroom.app.profile.ChangePasswordScreen
import com.musicroom.app.profile.EditProfileScreen
import com.musicroom.app.profile.ProfileRoute
import com.musicroom.app.profile.ProfileScreen
import com.musicroom.app.profile.SettingsSideBar
import com.musicroom.app.profile.SocialLinksScreen
import com.musicroom.app.profile.SpotifySettingsScreen
import com.musicroom.app.profile.TwoFactorSettingsScreen
import com.musicroom.app.search.SearchScreen
import com.musicroom.app.theme.ThemeMode
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.launch

/**
 * Logged-in part of the app: top bar, current tab, bottom bar, the player page (opened from the player bar)
 * and the profile pages (on large screens, next to the side menu and the settings categories).
 */
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
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var lastSettingsPage by remember { mutableStateOf(ProfileRoute.EditProfile) }
    // Keys of the pages that are not profile pages, for the page animations.
    val tabsKey = "tabs"
    val nowPlayingKey = "now_playing"
    val scope = rememberCoroutineScope()
    val player = remember(api) { PlayerController(api, scope) }
    // Songs the user adds to the queue show their profile picture.
    SideEffect { player.me = Proposer.of(user) }
    // Follows what Spotify plays while an account is linked.
    LaunchedEffect(user.spotifyConnected) { if (user.spotifyConnected) player.sync() }
    val onSpotifyConnected = { onUserChange(user.copy(spotifyConnected = true)) }
    val playerBar: @Composable () -> Unit = {
        AnimatedVisibility(
            visible = user.spotifyConnected && (player.track != null || player.error != null),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            PlayerBar(api, player, onOpen = { showNowPlaying = true })
        }
    }
    // Confirms songs added to the queue, at the bottom of the page (above the player bar).
    val queueToast: @Composable BoxScope.() -> Unit = {
        QueueToast(
            api,
            player,
            onOpen = if (showNowPlaying) null else ({ showNowPlaying = true }),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
    val openCreate = { showCreate = true }
    // The Create tab is not a page: it opens the create card from the bottom.
    val selectTab: (MainTab) -> Unit = { if (it == MainTab.Create) openCreate() else tab = it }

    // The player page shows while Spotify is linked; it closes with the account.
    val nowPlayingShown = showNowPlaying && user.spotifyConnected
    val closeNowPlaying = { showNowPlaying = false }

    // Android back: close the profile pages first, then the player page, then go back to the home tab.
    BackHandler(enabled = profileRoute != null || nowPlayingShown || tab != MainTab.Home) {
        val route = profileRoute
        when {
            route != null -> profileRoute = route.parent
            nowPlayingShown -> closeNowPlaying()
            else -> tab = MainTab.Home
        }
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

    val logout: () -> Unit = {
        scope.launch {
            api.logout()
            onLoggedOut()
        }
    }
    val wide = WindowSize.current == WindowSize.Expanded
    // Large screens: the settings menu stays on the left, so going back closes the settings.
    val back = { profileRoute = if (wide) null else profileRoute?.parent }
    val profilePage: @Composable (ProfileRoute) -> Unit = { route ->
        when (route) {
            ProfileRoute.Profile -> ProfileScreen(
                api = api,
                user = user,
                themeMode = themeMode,
                onBack = back,
                onOpen = { profileRoute = it },
                onLogout = logout,
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
    if (wide) {
        // Large screen: side menu on the left instead of the top and bottom bars. It stays visible on the
        // profile pages; a second menu with the settings categories opens next to it, and the chosen
        // page shows on the right in place of the tab (the account page first).
        val settingsPage = if (route == ProfileRoute.Profile) ProfileRoute.EditProfile else route
        Row(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
            SideNavBar(
                api,
                user,
                selected = if (route == null) tab else null,
                collapsed = sideNavCollapsed,
                onToggleCollapsed = { sideNavCollapsed = !sideNavCollapsed },
                onSelect = {
                    profileRoute = null
                    closeNowPlaying()
                    selectTab(it)
                },
                onOpenProfile = openProfile,
            )
            // The settings categories unfold next to the side menu.
            if (settingsPage != null) lastSettingsPage = settingsPage
            AnimatedVisibility(settingsPage != null, enter = expandHorizontally() + fadeIn(), exit = shrinkHorizontally() + fadeOut()) {
                SettingsSideBar(selected = lastSettingsPage, onOpen = { profileRoute = it }, onLogout = logout)
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    // Changing page fades the new one in while it rises a little, like the tabs.
                    AnimatedContent(
                        settingsPage?.name ?: if (nowPlayingShown) nowPlayingKey else tabsKey,
                        transitionSpec = {
                            (fadeIn(tween(220, delayMillis = 60)) + slideInVertically(tween(260)) { it / 30 })
                                .togetherWith(fadeOut(tween(120)))
                        },
                    ) { shown ->
                        when (shown) {
                            tabsKey -> Box(Modifier.fillMaxSize().safeDrawingPadding()) { content() }
                            nowPlayingKey -> NowPlayingScreen(api, player, onBack = closeNowPlaying)
                            else -> profilePage(ProfileRoute.valueOf(shown))
                        }
                    }
                    queueToast()
                }
                playerBar()
            }
        }
    } else {
        // Phones: profile pages replace the tabs (instead of covering them) so touches cannot reach the tabs
        // below. A deeper page slides in from the right and going back slides it away; the player page rises
        // from the bottom.
        fun depth(screen: String) = when (screen) {
            tabsKey -> 0
            nowPlayingKey -> 1
            else -> if (ProfileRoute.valueOf(screen).parent == null) 1 else 2
        }
        AnimatedContent(
            route?.name ?: if (nowPlayingShown) nowPlayingKey else tabsKey,
            transitionSpec = {
                val forward = depth(targetState) > depth(initialState)
                when {
                    nowPlayingKey in setOf(initialState, targetState) && tabsKey in setOf(initialState, targetState) ->
                        if (forward) {
                            slideInVertically(tween(320)) { it / 3 } + fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                        } else {
                            fadeIn(tween(220)) togetherWith slideOutVertically(tween(280)) { it / 3 } + fadeOut(tween(200))
                        }
                    forward ->
                        slideInHorizontally(tween(300)) { it } + fadeIn(tween(200)) togetherWith
                            slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(250))
                    else ->
                        (slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(250)) togetherWith
                            slideOutHorizontally(tween(300)) { it } + fadeOut(tween(200)))
                            .apply { targetContentZIndex = -1f }
                }
            },
        ) { shown ->
            when (shown) {
                tabsKey -> Column(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
                    MainTopBar(api, user, onOpenProfile = openProfile)
                    Box(Modifier.weight(1f).fillMaxWidth().imePadding()) {
                        content()
                        queueToast()
                    }
                    playerBar()
                    BottomNavBar(selected = tab, onSelect = selectTab)
                }
                nowPlayingKey -> Box(Modifier.fillMaxSize()) {
                    NowPlayingScreen(api, player, onBack = closeNowPlaying)
                    Box(Modifier.align(Alignment.BottomCenter).safeDrawingPadding()) { queueToast() }
                }
                else -> profilePage(ProfileRoute.valueOf(shown))
            }
        }
    }

    if (showCreate) {
        CreateSheet(
            api,
            onDismiss = { showCreate = false },
            onPlaylistCreated = { playlist ->
                showCreate = false
                profileRoute = null
                closeNowPlaying()
                openRoomPlaylist = playlist.id
                tab = MainTab.Playlists
            },
        )
    }
}
