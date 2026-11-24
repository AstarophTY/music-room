package com.musicroom.app.playlists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.ListPlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Settings
import com.musicroom.app.common.SectionTitle
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.common.UiText
import com.musicroom.app.common.rememberImagePicker
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.common_retry
import com.musicroom.app.generated.resources.messages_title
import com.musicroom.app.generated.resources.notifications_title
import com.musicroom.app.generated.resources.playlist_add_songs
import com.musicroom.app.generated.resources.playlist_move_down
import com.musicroom.app.generated.resources.playlist_move_up
import com.musicroom.app.generated.resources.playlist_no_songs
import com.musicroom.app.generated.resources.playlist_no_songs_editor
import com.musicroom.app.generated.resources.playlist_photo_change
import com.musicroom.app.generated.resources.playlist_settings
import com.musicroom.app.generated.resources.playlist_songs
import com.musicroom.app.generated.resources.playlists_by
import com.musicroom.app.generated.resources.playlists_play
import com.musicroom.app.generated.resources.playlists_track_count
import com.musicroom.app.generated.resources.queue_add
import com.musicroom.app.music.TrackMoreButton
import com.musicroom.app.music.TrackRow
import com.musicroom.app.player.PlayerController
import com.musicroom.app.player.Proposer
import com.musicroom.app.social.BadgedIconButton
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.ApiException
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.AvatarColor
import com.musicroom.shared.dto.PlaylistDetailResponse
import com.musicroom.shared.dto.PlaylistLiveEvent
import com.musicroom.shared.dto.PlaylistTrackResponse
import com.musicroom.shared.dto.SocialCountsResponse
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A Music Room playlist: photo (tap to change it when allowed), Play, its songs in order and its members.
 * Editors add songs from a card at the bottom, move and remove them; the owner also opens the settings.
 * The page follows the playlist live, so what other people change appears right away.
 * Tapping a song plays the playlist from it; the whole playlist or one song (swipe to the right, or its "...")
 * can be added to the queue, as proposed by who added it to the playlist, and a song to another playlist.
 */
@Composable
fun RoomPlaylistScreen(
    api: MusicRoomApi,
    playlistId: String,
    myUserId: String,
    player: PlayerController,
    counts: SocialCountsResponse,
    onOpenNotifications: () -> Unit,
    onOpenMessages: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var detail by remember(playlistId) { mutableStateOf<PlaylistDetailResponse?>(null) }
    var error by remember(playlistId) { mutableStateOf<UiText?>(null) }
    var attempt by remember { mutableStateOf(0) }
    var showAddSongs by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    // Songs there when the page opened just show; songs added afterwards slide in.
    var initialEntries by remember(playlistId) { mutableStateOf<Set<String>?>(null) }

    LaunchedEffect(playlistId, attempt) {
        error = null
        try {
            val loaded = api.roomPlaylist(playlistId)
            if (initialEntries == null) initialEntries = loaded.tracks.map { it.entryId }.toSet()
            detail = loaded
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
        }
    }

    // Answers and pushes can arrive out of order: an older revision never replaces a newer one.
    fun show(next: PlaylistDetailResponse) {
        val current = detail
        if (current == null || next.playlist.revision >= current.playlist.revision) detail = next
    }

    LaunchedEffect(playlistId) {
        var pause = 1_000L
        while (true) {
            try {
                api.playlistLive(playlistId) { event ->
                    pause = 1_000L
                    when (event) {
                        is PlaylistLiveEvent.State -> show(event.detail)
                        PlaylistLiveEvent.Gone -> onBack()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                if (e.status == 404) {
                    onBack()
                    return@LaunchedEffect
                }
            } catch (_: Exception) {
                // No network: the page keeps its last state and tries again.
            }
            delay(pause)
            pause = (pause * 2).coerceAtMost(30_000)
        }
    }

    fun run(action: suspend () -> PlaylistDetailResponse) {
        error = null
        scope.launch {
            try {
                show(action())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
            }
        }
    }

    val pickImage = rememberImagePicker { picked ->
        if (picked != null) run { api.uploadPlaylistCover(playlistId, picked.bytes, ContentType.parse(picked.mimeType)) }
    }

    val loaded = detail
    // Queued songs show who proposed them in the playlist.
    fun proposerOf(entry: PlaylistTrackResponse): Proposer? {
        val shown = detail ?: return null
        return (listOf(shown.playlist.owner) + shown.members).firstOrNull { it.userId == entry.addedById }?.let(Proposer::of)
            ?: entry.addedById?.let { Proposer(it, entry.addedBy, AvatarColor.YELLOW, null) }
    }
    AnimatedContent(
        targetState = showAddSongs && loaded != null,
        transitionSpec = {
            val opening = targetState
            (slideInHorizontally(tween(280)) { if (opening) it / 2 else -it / 4 } + fadeIn(tween(280)))
                .togetherWith(slideOutHorizontally(tween(220)) { if (opening) -it / 4 else it / 2 } + fadeOut(tween(180)))
        },
    ) { adding ->
        if (adding && loaded != null) {
            AddSongsPage(api, loaded, player, onChange = ::show, onBack = { showAddSongs = false })
        } else {
            SubScreenScaffold(
                title = loaded?.playlist?.name.orEmpty(),
                onBack = onBack,
                actions = {
                    BadgedIconButton(Lucide.Bell, stringResource(Res.string.notifications_title), counts.unreadNotifications, onOpenNotifications)
                    BadgedIconButton(Lucide.MessageCircle, stringResource(Res.string.messages_title), counts.unreadMessages, onOpenMessages)
                },
            ) {
                if (loaded == null) {
                    val failure = error
                    if (failure != null) {
                        Text(failure.resolve(), style = AppTheme.typography.bodySmall, color = colors.error)
                        AppButton(stringResource(Res.string.common_retry), { attempt++ }, variant = AppButtonVariant.Secondary)
                    } else {
                        Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    return@SubScreenScaffold
                }
                val playlist = loaded.playlist
                val tracks = loaded.tracks.map { it.track }

                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                    val editCover = if (playlist.canEdit) pickImage else null
                    Box(Modifier.size(132.dp).clip(AppShapes.large).then(if (editCover != null) Modifier.pressable(onClick = editCover) else Modifier)) {
                        PlaylistCover(api, playlist.id, playlist.coverVersion, tracks.firstOrNull()?.imageUrl, Modifier.size(132.dp))
                        if (editCover != null) {
                            Box(
                                Modifier.align(Alignment.BottomEnd).padding(AppTheme.spacing.sm).size(32.dp)
                                    .background(colors.surface, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Lucide.Camera, contentDescription = stringResource(Res.string.playlist_photo_change), tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                        PlaylistVisibilityBadge(playlist.visibility)
                        Text(playlist.name, style = AppTheme.typography.title, color = colors.textPrimary)
                        Text(
                            listOf(
                                stringResource(Res.string.playlists_by, playlist.owner.displayName),
                                pluralStringResource(Res.plurals.playlists_track_count, playlist.trackCount, playlist.trackCount),
                            ).joinToString(" · "),
                            style = AppTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    AppButton(stringResource(Res.string.playlists_play), { player.play(tracks, 0) }, enabled = tracks.isNotEmpty(), icon = Lucide.Play)
                    if (playlist.canEdit) {
                        AppButton(stringResource(Res.string.playlist_add_songs), { showAddSongs = true }, variant = AppButtonVariant.Secondary, icon = Lucide.Plus)
                    }
                    Row(Modifier.weight(1f)) {}
                    IconButton(onClick = { player.addProposed(loaded.tracks.map { it.track to proposerOf(it) }) }, enabled = tracks.isNotEmpty()) {
                        Icon(Lucide.ListPlus, contentDescription = stringResource(Res.string.queue_add), tint = colors.textPrimary)
                    }
                    if (playlist.isOwner) {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Lucide.Settings, contentDescription = stringResource(Res.string.playlist_settings), tint = colors.textPrimary)
                        }
                    }
                }
                error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }

                SectionTitle(stringResource(Res.string.playlist_songs))
                if (loaded.tracks.isEmpty()) {
                    Text(
                        stringResource(if (playlist.canEdit) Res.string.playlist_no_songs_editor else Res.string.playlist_no_songs),
                        style = AppTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
                Column {
                    loaded.tracks.forEachIndexed { index, entry ->
                        key(entry.entryId) {
                            val visible = remember { MutableTransitionState(initialEntries?.contains(entry.entryId) == true).apply { targetState = true } }
                            AnimatedVisibility(visible, enter = fadeIn() + expandVertically() + slideInHorizontally { it / 4 }) {
                                TrackRow(
                                    api,
                                    entry.track,
                                    playing = player.track?.id == entry.track.id,
                                    onClick = { player.play(tracks, index) },
                                    trailing = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (playlist.canEdit) {
                                                IconButton(
                                                    onClick = {
                                                        val after = loaded.tracks.getOrNull(index - 2)?.entryId
                                                        run { api.movePlaylistTrack(playlist.id, entry.entryId, after) }
                                                    },
                                                    enabled = index > 0,
                                                ) {
                                                    Icon(Lucide.ChevronUp, contentDescription = stringResource(Res.string.playlist_move_up), tint = colors.textSecondary)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val after = loaded.tracks[index + 1].entryId
                                                        run { api.movePlaylistTrack(playlist.id, entry.entryId, after) }
                                                    },
                                                    enabled = index < loaded.tracks.lastIndex,
                                                ) {
                                                    Icon(Lucide.ChevronDown, contentDescription = stringResource(Res.string.playlist_move_down), tint = colors.textSecondary)
                                                }
                                            }
                                            TrackMoreButton(
                                                api,
                                                player,
                                                entry.track,
                                                proposer = proposerOf(entry),
                                                onRemove = if (playlist.canEdit) {
                                                    { run { api.removePlaylistTrack(playlist.id, entry.entryId) } }
                                                } else {
                                                    null
                                                },
                                            )
                                        }
                                    },
                                    onSwipeRight = { player.addToQueue(listOf(entry.track), proposerOf(entry)) },
                                )
                            }
                        }
                    }
                }

                PlaylistMembersSection(api, loaded, onChange = ::show, onLeft = onBack, myUserId = myUserId)
            }
        }
    }

    val current = detail
    if (showSettings && current != null) {
        PlaylistSettingsSheet(api, current, onChange = ::show, onDeleted = onBack, onDismiss = { showSettings = false })
    }
}
