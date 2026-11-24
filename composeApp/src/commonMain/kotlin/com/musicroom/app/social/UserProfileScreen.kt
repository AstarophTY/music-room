package com.musicroom.app.social

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import com.musicroom.app.common.PersonAvatar
import com.musicroom.app.common.Reveal
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.common.UiText
import com.musicroom.app.common.icon
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.friend_count
import com.musicroom.app.generated.resources.message_write
import com.musicroom.app.generated.resources.social_section
import com.musicroom.app.generated.resources.user_profile_songs
import com.musicroom.app.generated.resources.profile_about
import com.musicroom.app.generated.resources.profile_for_friends
import com.musicroom.app.generated.resources.profile_music_tastes
import com.musicroom.app.generated.resources.user_profile_title
import com.musicroom.app.music.TrackMoreButton
import com.musicroom.app.music.TrackRow
import com.musicroom.app.player.PlayerController
import com.musicroom.app.profile.SettingsRow
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.FriendshipStatus
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.UserProfileResponse
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.stringResource

/**
 * Another user's page: photo, name, how many friends they have, their social links, and the friendship button.
 * Friends also get a button to chat. The songs they uploaded to Music Room come last, playable in the app.
 */
@Composable
fun UserProfileScreen(
    api: MusicRoomApi,
    social: SocialController,
    player: PlayerController,
    userId: String,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
) {
    val colors = AppTheme.colors
    val uriHandler = LocalUriHandler.current
    var profile by remember(userId) { mutableStateOf<UserProfileResponse?>(null) }
    var error by remember { mutableStateOf<UiText?>(null) }
    // The songs they uploaded to Music Room.
    var songs by remember(userId) { mutableStateOf<List<TrackResponse>>(emptyList()) }
    val pop = remember { Animatable(0f) }

    // Loaded again when the friendship changes from elsewhere (a request accepted on another device...).
    LaunchedEffect(userId, social.friends) {
        try {
            profile = api.userProfile(userId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
        }
    }
    LaunchedEffect(userId) {
        songs = try {
            api.userTracks(userId).items
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }
    LaunchedEffect(profile != null) {
        if (profile != null) pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 260f))
    }

    SubScreenScaffold(stringResource(Res.string.user_profile_title), onBack) {
        val shown = profile
        if (shown == null) {
            Text(
                error?.resolve() ?: stringResource(Res.string.common_loading),
                style = AppTheme.typography.bodySmall,
                color = if (error != null) colors.error else colors.textSecondary,
            )
            return@SubScreenScaffold
        }
        val user = shown.user
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            PersonAvatar(
                api,
                user.id,
                user.displayName,
                user.avatarColor,
                user.avatarVersion,
                size = 96.dp,
                modifier = Modifier.graphicsLayer {
                    val scale = 0.5f + 0.5f * pop.value
                    scaleX = scale
                    scaleY = scale
                    alpha = pop.value.coerceIn(0f, 1f)
                },
            )
            Reveal(1) { Text(user.displayName, style = AppTheme.typography.title, color = colors.textPrimary) }
            Reveal(2) {
                Text(stringResource(Res.string.friend_count, shown.friendCount), style = AppTheme.typography.body, color = colors.textSecondary)
            }
        }
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
        Reveal(3) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                FriendButton(
                    social,
                    user,
                    onChange = { status ->
                        error = null
                        val count = shown.friendCount + when {
                            status == FriendshipStatus.FRIENDS && user.friendship != FriendshipStatus.FRIENDS -> 1
                            status != FriendshipStatus.FRIENDS && user.friendship == FriendshipStatus.FRIENDS -> -1
                            else -> 0
                        }
                        profile = shown.copy(user = user.copy(friendship = status), friendCount = count)
                    },
                    onError = { error = it },
                    modifier = Modifier.weight(1f),
                )
                if (user.friendship == FriendshipStatus.FRIENDS) {
                    AppButton(
                        stringResource(Res.string.message_write),
                        { onOpenChat(user.id) },
                        Modifier.weight(1f),
                        variant = AppButtonVariant.Primary,
                        icon = Lucide.MessageCircle,
                    )
                }
            }
        }
        listOf(
            stringResource(Res.string.profile_about) to shown.publicInfo,
            stringResource(Res.string.profile_for_friends) to shown.friendsInfo.orEmpty(),
            stringResource(Res.string.profile_music_tastes) to shown.musicPreferences.joinToString(" · "),
        ).filter { it.second.isNotEmpty() }.forEach { (title, body) ->
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                Text(title, style = AppTheme.typography.label, color = colors.textSecondary)
                Text(body, style = AppTheme.typography.body, color = colors.textPrimary)
            }
        }
        if (shown.socialLinks.isNotEmpty()) {
            Reveal(4) {
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                    Text(stringResource(Res.string.social_section), style = AppTheme.typography.label, color = colors.textSecondary)
                    AppCard(Modifier.fillMaxWidth()) {
                        shown.socialLinks.forEach { link ->
                            val url = link.network.urlFor(link.handle)
                            SettingsRow(
                                link.network.icon(),
                                link.network.displayName,
                                onClick = { if (url != null) runCatching { uriHandler.openUri(url) } },
                                value = "@${link.handle}",
                                showChevron = url != null,
                            )
                        }
                    }
                }
            }
        }
        if (songs.isNotEmpty()) {
            Reveal(5) {
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    Text(stringResource(Res.string.user_profile_songs), style = AppTheme.typography.label, color = colors.textSecondary)
                    Column {
                        songs.forEachIndexed { index, song ->
                            TrackRow(
                                api,
                                song,
                                playing = player.track?.id == song.id,
                                onClick = { player.play(songs, index) },
                                trailing = { TrackMoreButton(api, player, song) },
                                onSwipeRight = { player.addToQueue(listOf(song)) },
                            )
                        }
                    }
                }
            }
        }
    }
}
