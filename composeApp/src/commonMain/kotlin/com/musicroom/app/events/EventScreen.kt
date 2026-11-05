package com.musicroom.app.events

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.CircleStop
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.HeadphoneOff
import com.composables.icons.lucide.Headphones
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.SkipForward
import com.composables.icons.lucide.UserPlus
import com.composables.icons.lucide.Users
import com.composables.icons.lucide.Vote
import com.musicroom.app.common.EmptyState
import com.musicroom.app.common.PersonAvatar
import com.musicroom.app.common.UiText
import com.musicroom.app.common.rememberLocationProvider
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_back
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.event_by
import com.musicroom.app.generated.resources.event_cannot_vote_closed
import com.musicroom.app.generated.resources.event_cannot_vote_invited
import com.musicroom.app.generated.resources.event_host_streams
import com.musicroom.app.generated.resources.event_invite_title
import com.musicroom.app.generated.resources.event_invited_people
import com.musicroom.app.generated.resources.event_license_area_short
import com.musicroom.app.generated.resources.event_license_area_until
import com.musicroom.app.generated.resources.event_license_everyone_short
import com.musicroom.app.generated.resources.event_license_invited_short
import com.musicroom.app.generated.resources.event_focus
import com.musicroom.app.generated.resources.event_focus_hint
import com.musicroom.app.generated.resources.event_focus_premium
import com.musicroom.app.generated.resources.event_unfocus
import com.musicroom.app.generated.resources.event_listeners
import com.musicroom.app.generated.resources.event_next
import com.musicroom.app.generated.resources.event_pause
import com.musicroom.app.generated.resources.event_propose
import com.musicroom.app.generated.resources.event_propose_added
import com.musicroom.app.generated.resources.event_propose_title
import com.musicroom.app.generated.resources.event_queue
import com.musicroom.app.generated.resources.event_queue_empty_body
import com.musicroom.app.generated.resources.event_queue_empty_title
import com.musicroom.app.generated.resources.event_resume
import com.musicroom.app.generated.resources.event_settings
import com.musicroom.app.generated.resources.event_skip
import com.musicroom.app.generated.resources.event_start
import com.musicroom.app.generated.resources.event_stop
import com.musicroom.app.generated.resources.event_visibility_private
import com.musicroom.app.generated.resources.event_visibility_public
import com.musicroom.app.music.TrackPickerSheet
import com.musicroom.app.player.PlayerController
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.ApiException
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.EventStatus
import com.musicroom.shared.dto.EventVisibility
import com.musicroom.shared.dto.EventVoteLicense
import com.musicroom.shared.dto.UserResponse
import org.jetbrains.compose.resources.stringResource

/**
 * An event, live: the song playing on the owner's Spotify, the one chosen to follow, and the queue where people
 * propose songs and vote (the most voted plays next; the list reorders itself as votes come). Only the owner needs
 * Spotify; the others can still listen on their own Spotify. The owner starts, pauses, skips and stops the music,
 * invites friends and changes the settings.
 */
@Composable
fun EventScreen(
    api: MusicRoomApi,
    user: UserResponse,
    player: PlayerController,
    eventId: String,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val room = remember(eventId) { EventRoom(api, eventId, scope) }
    val locate = rememberLocationProvider()
    var showPicker by remember { mutableStateOf(false) }
    var showInvite by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    SideEffect {
        room.locate = locate
    }
    LaunchedEffect(room) { room.run() }
    LaunchedEffect(room.deleted) { if (room.deleted) onBack() }

    val pill: @Composable (ImageVector, String) -> Unit = { icon, text ->
        Row(
            Modifier.background(colors.surfaceSubtle, AppShapes.pill).padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
            Text(text, style = AppTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1)
        }
    }

    Box(Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.TopCenter) {
        val detail = room.detail
        if (detail == null) {
            Column(Modifier.fillMaxWidth().padding(AppTheme.spacing.md)) {
                IconButton(onClick = onBack) {
                    Icon(Lucide.ArrowLeft, contentDescription = stringResource(Res.string.common_back), tint = colors.textPrimary)
                }
                Text(
                    room.error?.resolve() ?: stringResource(Res.string.common_loading),
                    style = AppTheme.typography.bodySmall,
                    color = if (room.error != null) colors.error else colors.textSecondary,
                )
            }
            return@Box
        }
        val event = detail.event
        LazyColumn(
            Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Lucide.ArrowLeft, contentDescription = stringResource(Res.string.common_back), tint = colors.textPrimary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(event.name, style = AppTheme.typography.title, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Row(
                                Modifier.pressable(onClick = { if (!event.isOwner) onOpenUser(event.owner.id) }),
                                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PersonAvatar(api, event.owner.id, event.owner.displayName, event.owner.avatarColor, event.owner.avatarVersion, size = 18.dp)
                                Text(stringResource(Res.string.event_by, event.owner.displayName), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                        }
                        if (event.isOwner) {
                            IconButton(onClick = { showInvite = true }) {
                                Icon(Lucide.UserPlus, contentDescription = stringResource(Res.string.event_invite_title), tint = colors.textPrimary)
                            }
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Lucide.Settings, contentDescription = stringResource(Res.string.event_settings), tint = colors.textPrimary)
                            }
                        }
                    }
                    if (event.description.isNotBlank()) {
                        Text(event.description, style = AppTheme.typography.body, color = colors.textSecondary)
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                        if (event.visibility == EventVisibility.PUBLIC) {
                            pill(Lucide.Globe, stringResource(Res.string.event_visibility_public))
                        } else {
                            pill(Lucide.Lock, stringResource(Res.string.event_visibility_private))
                        }
                        when (event.voteLicense) {
                            EventVoteLicense.EVERYONE -> pill(Lucide.Vote, stringResource(Res.string.event_license_everyone_short))
                            EventVoteLicense.INVITED -> pill(Lucide.Users, stringResource(Res.string.event_license_invited_short))
                            EventVoteLicense.LOCATION -> {
                                val radius = event.radiusMeters ?: 0
                                val area = if (radius >= 1_000) "${radius / 1_000} km" else "$radius m"
                                val end = event.endsAt
                                pill(
                                    Lucide.MapPin,
                                    if (end != null) {
                                        stringResource(Res.string.event_license_area_until, area, clockTime(end))
                                    } else {
                                        stringResource(Res.string.event_license_area_short, area)
                                    },
                                )
                            }
                        }
                        pill(Lucide.Headphones, stringResource(Res.string.event_listeners, event.listeners))
                    }
                    if (!event.canVote) {
                        Text(
                            stringResource(
                                if (event.voteLicense == EventVoteLicense.INVITED) Res.string.event_cannot_vote_invited else Res.string.event_cannot_vote_closed,
                            ),
                            style = AppTheme.typography.bodySmall,
                            color = colors.warning,
                        )
                    }
                }
            }

            item(key = "now") {
                EventNowPlaying(api, detail) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                        if (event.isOwner) {
                            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                                when (event.status) {
                                    EventStatus.PLAYING -> AppButton(
                                        stringResource(Res.string.event_pause),
                                        room::pause,
                                        Modifier.weight(1f),
                                        enabled = !room.busy,
                                        variant = AppButtonVariant.Secondary,
                                        icon = Lucide.Pause,
                                    )
                                    else -> AppButton(
                                        stringResource(if (event.status == EventStatus.PAUSED) Res.string.event_resume else Res.string.event_start),
                                        room::play,
                                        Modifier.weight(1f),
                                        enabled = !room.busy,
                                        icon = Lucide.Play,
                                    )
                                }
                                if (event.status != EventStatus.WAITING) {
                                    AppButton(
                                        stringResource(Res.string.event_skip),
                                        room::skip,
                                        Modifier.weight(1f),
                                        enabled = !room.busy,
                                        variant = AppButtonVariant.Secondary,
                                        icon = Lucide.SkipForward,
                                    )
                                }
                            }
                            if (event.status != EventStatus.WAITING) {
                                AppButton(
                                    stringResource(Res.string.event_stop),
                                    room::stop,
                                    Modifier.fillMaxWidth(),
                                    enabled = !room.busy,
                                    variant = AppButtonVariant.Ghost,
                                    icon = Lucide.CircleStop,
                                )
                            }
                            detail.playbackIssue?.let { code ->
                                Text(UiText.of(ApiException(409, code, null)).resolve(), style = AppTheme.typography.bodySmall, color = colors.error)
                            }
                        } else {
                            Text(
                                stringResource(Res.string.event_host_streams, event.owner.displayName),
                                style = AppTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                        // Listening to the event: the user's Spotify follows its music, and swiped songs go to it.
                        val focus = EventFocus.Local.current
                        if (focus != null) {
                            val focused = focus.event?.id == event.id
                            AppButton(
                                stringResource(if (focused) Res.string.event_unfocus else Res.string.event_focus),
                                { if (focused) focus.unfocus() else focus.focus(event) },
                                Modifier.fillMaxWidth(),
                                enabled = focused || focus.canFocus,
                                variant = if (focused) AppButtonVariant.Ghost else AppButtonVariant.Secondary,
                                icon = if (focused) Lucide.HeadphoneOff else Lucide.Headphones,
                            )
                            if (!focused) {
                                Text(
                                    stringResource(if (focus.canFocus) Res.string.event_focus_hint else Res.string.event_focus_premium),
                                    style = AppTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                )
                            }
                        }
                    }
                }
            }

            room.error?.let { error ->
                item(key = "error") { Text(error.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
            }

            detail.next?.let { next ->
                item(key = "next-title") { Text(stringResource(Res.string.event_next), style = AppTheme.typography.label, color = colors.textSecondary) }
                item(key = next.id) { AppCard(Modifier.fillMaxWidth().animateItem()) { EventTrackRow(api, next, rank = null) } }
            }

            item(key = "queue-title") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.event_queue, detail.queue.size),
                        style = AppTheme.typography.heading,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (event.canVote) {
                        AppButton(stringResource(Res.string.event_propose), { showPicker = true }, icon = Lucide.Plus)
                    }
                }
            }
            if (detail.queue.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        Lucide.Vote,
                        stringResource(Res.string.event_queue_empty_title),
                        stringResource(Res.string.event_queue_empty_body),
                        actionLabel = if (event.canVote) stringResource(Res.string.event_propose) else null,
                        onAction = { showPicker = true },
                    )
                }
            }
            itemsIndexed(detail.queue, key = { _, entry -> entry.id }) { index, entry ->
                val mine = entry.proposedBy?.id == user.id
                EventTrackRow(
                    api,
                    entry,
                    rank = index + 1,
                    modifier = Modifier.animateItem(),
                    onRemove = if (event.isOwner || mine) ({ room.remove(entry.id) }) else null,
                ) {
                    VoteButton(entry.votes, entry.voted, enabled = event.canVote, onClick = { room.toggleVote(entry.id) })
                }
            }

            if (detail.invited.isNotEmpty()) {
                item(key = "invited") {
                    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), modifier = Modifier.padding(top = AppTheme.spacing.md)) {
                        Text(stringResource(Res.string.event_invited_people, detail.invited.size), style = AppTheme.typography.label, color = colors.textSecondary)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                            detail.invited.forEach { person ->
                                PersonAvatar(
                                    api,
                                    person.id,
                                    person.displayName,
                                    person.avatarColor,
                                    person.avatarVersion,
                                    modifier = Modifier.pressable(onClick = { if (person.id != user.id) onOpenUser(person.id) }),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val shown = room.detail
    if (showPicker && shown != null) {
        val pendingIds = (shown.queue + listOfNotNull(shown.next, shown.nowPlaying)).map { it.track.id }.toSet()
        TrackPickerSheet(
            api,
            player,
            title = stringResource(Res.string.event_propose_title),
            addLabel = stringResource(Res.string.event_propose),
            addedLabel = stringResource(Res.string.event_propose_added),
            isAdded = { it.id in pendingIds },
            onAdd = { room.propose(it) },
            onDismiss = { showPicker = false },
            showTopTracks = user.spotifyConnected,
        )
    }
    if (showInvite) EventInviteSheet(api, room, onDismiss = { showInvite = false })
    if (showSettings && shown != null) {
        EventSettingsSheet(api, room, shown.event, onDismiss = { showSettings = false }, onDeleted = {
            showSettings = false
            onBack()
        })
    }
}
