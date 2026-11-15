package com.musicroom.app.events

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Ticket
import com.musicroom.app.common.EmptyState
import com.musicroom.app.common.Reveal
import com.musicroom.app.common.TabContent
import com.musicroom.app.common.UiText
import com.musicroom.app.common.rememberLocationProvider
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.event_location_failed
import com.musicroom.app.generated.resources.events_create
import com.musicroom.app.generated.resources.events_empty_body
import com.musicroom.app.generated.resources.events_empty_title
import com.musicroom.app.generated.resources.events_nearby_empty
import com.musicroom.app.generated.resources.events_nearby_unavailable
import com.musicroom.app.generated.resources.events_subtitle
import com.musicroom.app.player.PlayerController
import com.musicroom.app.theme.components.AppChip
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.GeoDistance
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.EventResponse
import com.musicroom.shared.dto.GeoPoint
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * Events: public ones (playing first), those nearby (by the user's position), those the user was invited to, and
 * their own. Opening one shows its page in place of the list ([openEventId]); back returns to the list.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EventsScreen(
    api: MusicRoomApi,
    user: UserResponse,
    player: PlayerController,
    openEventId: String?,
    onOpenEvent: (String?) -> Unit,
    onOpenUser: (String) -> Unit,
    onCreate: () -> Unit,
) {
    val colors = AppTheme.colors
    val locate = rememberLocationProvider()
    var filter by rememberSaveable { mutableStateOf(EventFilter.Public) }
    var events by remember { mutableStateOf<List<EventResponse>?>(null) }
    var here by remember { mutableStateOf<GeoPoint?>(null) }
    var error by remember { mutableStateOf<UiText?>(null) }
    BackHandler(enabled = openEventId != null) { onOpenEvent(null) }

    // Reloaded when the filter changes, when back from an event, and every 20 s (live badges, listeners).
    LaunchedEffect(filter, openEventId) {
        if (openEventId != null) return@LaunchedEffect
        events = null
        error = null
        while (true) {
            try {
                val near = if (filter == EventFilter.Nearby) {
                    val provider = locate
                    if (provider == null) {
                        error = UiText.of(Res.string.events_nearby_unavailable)
                        events = emptyList()
                        return@LaunchedEffect
                    }
                    (here ?: provider())?.also { here = it } ?: run {
                        error = UiText.of(Res.string.event_location_failed)
                        events = emptyList()
                        return@LaunchedEffect
                    }
                } else {
                    null
                }
                events = api.events(filter.scope, near).items
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (events == null) events = emptyList()
                error = UiText.of(e)
            }
            delay(20_000)
        }
    }

    AnimatedContent(
        openEventId,
        transitionSpec = {
            val opening = targetState != null
            (slideInHorizontally(tween(280)) { if (opening) it / 2 else -it / 4 } + fadeIn(tween(280)))
                .togetherWith(slideOutHorizontally(tween(220)) { if (opening) -it / 4 else it / 2 } + fadeOut(tween(180)))
        },
    ) { shownEvent ->
        if (shownEvent != null) {
            EventScreen(api, user, player, shownEvent, onBack = { onOpenEvent(null) }, onOpenUser = onOpenUser)
            return@AnimatedContent
        }
        TabContent {
            Text(stringResource(Res.string.events_subtitle), style = AppTheme.typography.body, color = colors.textSecondary)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                EventFilter.entries.forEach { option ->
                    AppChip(stringResource(option.label), selected = option == filter, onClick = { filter = option })
                }
            }
            error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
            val list = events
            when {
                list == null -> Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                list.isEmpty() && filter == EventFilter.Nearby && error == null -> EmptyState(
                    Lucide.MapPin,
                    stringResource(Res.string.events_empty_title),
                    stringResource(Res.string.events_nearby_empty),
                    actionLabel = stringResource(Res.string.events_create),
                    onAction = onCreate,
                )
                list.isEmpty() -> EmptyState(
                    Lucide.Ticket,
                    stringResource(Res.string.events_empty_title),
                    stringResource(Res.string.events_empty_body),
                    actionLabel = stringResource(Res.string.events_create),
                    onAction = onCreate,
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    list.forEachIndexed { index, event ->
                        val distance = here?.takeIf { filter == EventFilter.Nearby }?.let { me ->
                            event.location?.let { GeoDistance.meters(me, it) }
                        }?.let { meters -> if (meters >= 1_000) "${(meters / 100).toInt() / 10.0} km" else "${meters.toInt()} m" }
                        Reveal(index.coerceAtMost(6)) {
                            EventCard(api, event, onClick = { onOpenEvent(event.id) }, distance = distance)
                        }
                    }
                }
            }
        }
    }
}
