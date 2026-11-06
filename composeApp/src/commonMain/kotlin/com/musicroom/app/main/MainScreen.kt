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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import com.musicroom.app.common.rememberLocationProvider
import com.musicroom.app.create.CreateSheet
import com.musicroom.app.events.EventFocus
import com.musicroom.app.events.EventFocusBar
import com.musicroom.app.events.EventsScreen
import com.musicroom.app.home.HomeScreen
import com.musicroom.app.player.NowPlayingScreen
import com.musicroom.app.player.PlayerBar
import com.musicroom.app.player.PlayerController
import com.musicroom.app.music.AppleMusicPlayback
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
import com.musicroom.app.profile.MySongsScreen
import com.musicroom.app.profile.PremiumScreen
import com.musicroom.app.profile.AudioProvidersScreen
import com.musicroom.app.profile.TwoFactorSettingsScreen
import com.musicroom.app.search.SearchScreen
import com.musicroom.app.social.ChatScreen
import com.musicroom.app.social.ConversationsScreen
import com.musicroom.app.social.FriendsScreen
import com.musicroom.app.social.NotificationBanner
import com.musicroom.app.social.NotificationsScreen
import com.musicroom.app.social.SocialController
import com.musicroom.app.social.SocialPage
import com.musicroom.app.social.UserProfileScreen
import com.musicroom.app.theme.ThemeMode
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Logged-in part of the app: top bar, current tab, bottom bar, the player page (opened from the player bar),
 * the profile pages (on large screens, next to the side menu and the settings categories) and the social pages
 * (friends, chats, notifications, other users' profiles), with a banner when a notification arrives.
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
    // The event open in the Events tab.
    var openEvent by rememberSaveable { mutableStateOf<String?>(null) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var lastSettingsPage by remember { mutableStateOf(ProfileRoute.EditProfile) }
    // Social pages opened over the tabs (friends, chats, notifications, profiles), the last one on top: SocialPage keys.
    var socialPages by rememberSaveable { mutableStateOf(listOf<String>()) }
    // Keys of the pages that are not profile pages, for the page animations.
    val tabsKey = "tabs"
    val nowPlayingKey = "now_playing"
    val scope = rememberCoroutineScope()
    val appleMusicPlayback = AppleMusicPlayback.Local.current
    val player = remember(api, appleMusicPlayback) { PlayerController(api, scope, appleMusicPlayback) }
    val social = remember(api, user.id) { SocialController(api, user.id, scope) }
    // The event the user listens to: its music plays on their Spotify too, and swiped songs are proposed to it.
    val eventFocus = remember(api) { EventFocus(api, player, scope) }
    val locate = rememberLocationProvider()
    LaunchedEffect(eventFocus) { eventFocus.run() }
    // An uploaded song plays in the app: it stops with the signed-in screens (logout).
    DisposableEffect(player) { onDispose { player.release() } }
    // Listening to an event plays on the user's Spotify: only with Premium.
    LaunchedEffect(eventFocus, user.spotifyConnected) {
        val status = if (!user.spotifyConnected) {
            null
        } else {
            try {
                api.spotifyStatus()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
        eventFocus.updateCanFocus(status?.connected == true && status.product == "premium")
    }
    // Live friends, messages and notifications while logged in.
    LaunchedEffect(social) { social.run() }
    // Songs the user adds to the queue show their profile picture.
    SideEffect {
        player.me = Proposer.of(user)
        // Without Spotify, playing a song asks to link it.
        player.spotifyConnected = user.spotifyConnected
        eventFocus.locate = locate
    }
    // Follows what Spotify plays while an account is linked.
    LaunchedEffect(user.spotifyConnected) { if (user.spotifyConnected) player.sync() }
    val playerBar: @Composable () -> Unit = {
        Column {
            EventFocusBar(eventFocus, onOpen = { id ->
                profileRoute = null
                socialPages = emptyList()
                showNowPlaying = false
                openEvent = id
                tab = MainTab.Events
            })
            AnimatedVisibility(
                // Uploaded songs play without Spotify.
                visible = ((user.spotifyConnected || player.playingHosted || player.playingAppleMusic) && player.track != null) || player.error != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                PlayerBar(api, player, onOpen = { showNowPlaying = true })
            }
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
    // The Create tab is not a page: it opens the create card from the bottom. Choosing a tab closes the social pages.
    val selectTab: (MainTab) -> Unit = {
        if (it == MainTab.Create) {
            openCreate()
        } else {
            socialPages = emptyList()
            tab = it
        }
    }

    // The player page shows while Spotify is linked (or an uploaded song plays); it closes with the account.
    val nowPlayingShown = showNowPlaying && (user.spotifyConnected || player.playingHosted || player.playingAppleMusic)
    val closeNowPlaying = { showNowPlaying = false }

    // Opens a social page on top of the others; one already open comes back to the top.
    val openSocial: (SocialPage) -> Unit = { page ->
        profileRoute = null
        showNowPlaying = false
        val index = socialPages.indexOf(page.key)
        socialPages = if (index >= 0) socialPages.take(index + 1) else socialPages + page.key
    }
    val socialBack: () -> Unit = { socialPages = socialPages.dropLast(1) }
    val closeSocialAnd: (() -> Unit) -> Unit = { then ->
        socialPages = emptyList()
        then()
    }

    // Android back: close the profile pages first, then the social pages, then the player page, then go back to
    // the home tab.
    BackHandler(enabled = profileRoute != null || socialPages.isNotEmpty() || nowPlayingShown || tab != MainTab.Home) {
        val route = profileRoute
        when {
            route != null -> profileRoute = route.parent
            socialPages.isNotEmpty() -> socialBack()
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
                onCreate = openCreate,
            )
            MainTab.Search -> SearchScreen(
                api,
                user,
                player,
                social,
                onOpenUser = { openSocial(SocialPage.Profile(it)) },
            )
            MainTab.Events -> EventsScreen(
                api,
                user,
                player,
                openEventId = openEvent,
                onOpenEvent = { openEvent = it },
                onOpenUser = { openSocial(SocialPage.Profile(it)) },
                onCreate = openCreate,
            )
            MainTab.Playlists -> PlaylistsScreen(
                api,
                user,
                player,
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
                pendingRequests = social.counts.pendingRequests,
                onBack = back,
                onOpen = { profileRoute = it },
                onOpenFriends = { openSocial(SocialPage.Friends) },
                onLogout = logout,
            )
            ProfileRoute.EditProfile -> EditProfileScreen(api, user, onUserChange, onBack = back)
            ProfileRoute.SocialLinks -> SocialLinksScreen(api, user, onUserChange, onBack = back)
            ProfileRoute.ChangePassword -> ChangePasswordScreen(api, onBack = back)
            ProfileRoute.TwoFactor -> TwoFactorSettingsScreen(api, user, onUserChange, onBack = back)
            ProfileRoute.Appearance -> AppearanceScreen(themeMode, onThemeModeChange, onBack = back)
            ProfileRoute.Spotify -> AudioProvidersScreen(api, user, onUserChange, onBack = back)
            ProfileRoute.MySongs -> MySongsScreen(api, user, player, onOpenPremium = { profileRoute = ProfileRoute.Premium }, onBack = back)
            ProfileRoute.Premium -> PremiumScreen(api, onBack = back)
        }
    }

    val openUser: (String) -> Unit = { openSocial(SocialPage.Profile(it)) }
    val openChat: (String) -> Unit = { openSocial(SocialPage.Chat(it)) }
    val findPeople = { closeSocialAnd { tab = MainTab.Search } }
    val socialPage: @Composable (SocialPage) -> Unit = { page ->
        when (page) {
            SocialPage.Friends -> FriendsScreen(api, social, socialBack, openUser, openChat, onFindPeople = findPeople)
            SocialPage.Notifications -> NotificationsScreen(
                api,
                social,
                socialBack,
                openUser,
                onOpenPlaylist = { id ->
                    closeSocialAnd {
                        openRoomPlaylist = id
                        tab = MainTab.Playlists
                    }
                },
                onOpenEvent = { id ->
                    closeSocialAnd {
                        openEvent = id
                        tab = MainTab.Events
                    }
                },
            )
            SocialPage.Conversations -> ConversationsScreen(
                api,
                social,
                user.id,
                socialBack,
                openChat,
                onOpenFriends = { openSocial(SocialPage.Friends) },
                onFindPeople = findPeople,
            )
            is SocialPage.Chat -> ChatScreen(api, social, user.id, page.userId, socialBack, openUser)
            is SocialPage.Profile -> UserProfileScreen(api, social, player, page.userId, socialBack, openChat)
        }
    }
    val socialKey = socialPages.lastOrNull()
    // The live banner of a new notification, hidden on the notifications page itself.
    val banner: @Composable BoxScope.() -> Unit = {
        if (socialKey != SocialPage.Notifications.key) {
            NotificationBanner(
                api,
                social,
                onOpen = { openSocial(SocialPage.Notifications) },
                modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding(),
            )
        }
    }

    val route = profileRoute
    CompositionLocalProvider(EventFocus.Local provides eventFocus) {
        Box(Modifier.fillMaxSize()) {
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
                        counts = social.counts,
                        socialOpen = if (route == null) socialPages.firstOrNull()?.let(SocialPage::parse) else null,
                        onOpenNotifications = {
                            socialPages = emptyList()
                            openSocial(SocialPage.Notifications)
                        },
                        onOpenMessages = {
                            socialPages = emptyList()
                            openSocial(SocialPage.Conversations)
                        },
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
                                settingsPage?.name ?: socialKey ?: if (nowPlayingShown) nowPlayingKey else tabsKey,
                                transitionSpec = {
                                    (fadeIn(tween(220, delayMillis = 60)) + slideInVertically(tween(260)) { it / 30 })
                                        .togetherWith(fadeOut(tween(120)))
                                },
                            ) { shown ->
                                when (shown) {
                                    tabsKey -> Box(Modifier.fillMaxSize().safeDrawingPadding()) { content() }
                                    nowPlayingKey -> NowPlayingScreen(api, player, onBack = closeNowPlaying)
                                    else -> SocialPage.parse(shown)?.let { socialPage(it) } ?: profilePage(ProfileRoute.valueOf(shown))
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
                fun depth(screen: String) = when {
                    screen == tabsKey -> 0
                    screen == nowPlayingKey -> 1
                    // A social page that was just closed is no longer in the list: it was the deepest.
                    SocialPage.parse(screen) != null -> 1 + socialPages.indexOf(screen).let { if (it < 0) socialPages.size else it }
                    else -> if (ProfileRoute.valueOf(screen).parent == null) 1 else 2
                }
                AnimatedContent(
                    route?.name ?: socialKey ?: if (nowPlayingShown) nowPlayingKey else tabsKey,
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
                            MainTopBar(
                                api,
                                user,
                                counts = social.counts,
                                onOpenProfile = openProfile,
                                onOpenNotifications = { openSocial(SocialPage.Notifications) },
                                onOpenMessages = { openSocial(SocialPage.Conversations) },
                            )
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
                        else -> SocialPage.parse(shown)?.let { socialPage(it) } ?: profilePage(ProfileRoute.valueOf(shown))
                    }
                }
            }
            banner()
        }
    }

    if (showCreate) {
        CreateSheet(
            api,
            spotifyConnected = user.spotifyConnected,
            onOpenProviders = { showCreate = false; profileRoute = ProfileRoute.Spotify },
            onDismiss = { showCreate = false },
            onPlaylistCreated = { playlist ->
                showCreate = false
                profileRoute = null
                socialPages = emptyList()
                closeNowPlaying()
                openRoomPlaylist = playlist.id
                tab = MainTab.Playlists
            },
            onEventCreated = { id ->
                showCreate = false
                profileRoute = null
                socialPages = emptyList()
                closeNowPlaying()
                openEvent = id
                tab = MainTab.Events
            },
        )
    }
}
