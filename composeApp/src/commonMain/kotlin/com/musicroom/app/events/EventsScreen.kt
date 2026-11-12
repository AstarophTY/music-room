package com.musicroom.app.events

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Brush
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Plus
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
import com.musicroom.app.generated.resources.events_map_none
import com.musicroom.app.generated.resources.events_nearby_empty
import com.musicroom.app.generated.resources.events_nearby_unavailable
import com.musicroom.app.generated.resources.events_subtitle
import com.musicroom.app.generated.resources.events_without_place
import com.musicroom.app.map.EventsMap
import com.musicroom.app.music.MusicProviderConnection
import com.musicroom.app.player.PlayerController
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
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
    providers: List<MusicProviderConnection>,
    onOpenProviders: () -> Unit,
) {
    val colors = AppTheme.colors
    val locate = rememberLocationProvider()
    var filter by rememberSaveable { mutableStateOf(EventFilter.Public) }
    var onMap by rememberSaveable { mutableStateOf(true) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var events by remember { mutableStateOf<List<EventResponse>?>(null) }
    var here by remember { mutableStateOf<GeoPoint?>(null) }
    var error by remember { mutableStateOf<UiText?>(null) }
    BackHandler(enabled = openEventId != null) { onOpenEvent(null) }

    // Where the user is opens the map and measures the distances; only the nearby filter cannot do without it.
    LaunchedEffect(locate) {
        val provider = locate ?: return@LaunchedEffect
        if (here == null) {
            here = try {
                provider()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
    }

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
        val list = events
        val filters: @Composable (Modifier) -> Unit = { modifier ->
            EventFilterBar(filter, onFilter = { filter = it }, onMap = onMap, onToggleView = { onMap = !onMap }, modifier = modifier)
        }
        Box(Modifier.fillMaxSize()) {
            if (onMap) {
                EventsMap(api, list.orEmpty(), here, onOpenEvent, Modifier.fillMaxSize())
                // The map runs under the filters: the background holds behind them, then fades well below
                // rather than stopping at their edge, which would draw a line across the map.
                Column(
                    Modifier.fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                0f to colors.background,
                                0.5f to colors.background,
                                1f to colors.background.copy(alpha = 0f),
                            ),
                        )
                        .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm)
                        .padding(bottom = AppTheme.spacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                ) {
                    filters(Modifier.fillMaxWidth())
                    error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
                    val missing = list?.count { it.location == null } ?: 0
                    if (list != null && list.none { it.location != null }) {
                        Text(stringResource(Res.string.events_map_none), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                    } else if (missing > 0) {
                        Text(stringResource(Res.string.events_without_place, missing), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }
                AppButton(
                    stringResource(Res.string.events_create),
                    onClick = { creating = true },
                    modifier = Modifier.align(Alignment.BottomStart).padding(AppTheme.spacing.md),
                    icon = Lucide.Plus,
                )
            } else {
                TabContent {
                    Text(stringResource(Res.string.events_subtitle), style = AppTheme.typography.body, color = colors.textSecondary)
                    filters(Modifier.fillMaxWidth())
                    // The map has it floating in a corner; here it sits where the list begins.
                    if (!list.isNullOrEmpty()) {
                        AppButton(
                            stringResource(Res.string.events_create),
                            onClick = { creating = true },
                            variant = AppButtonVariant.Secondary,
                            icon = Lucide.Plus,
                        )
                    }
                    error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
                    when {
                        list == null -> Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                        list.isEmpty() && filter == EventFilter.Nearby && error == null -> EmptyState(
                            Lucide.MapPin,
                            stringResource(Res.string.events_empty_title),
                            stringResource(Res.string.events_nearby_empty),
                            actionLabel = stringResource(Res.string.events_create),
                            onAction = { creating = true },
                        )
                        list.isEmpty() -> EmptyState(
                            Lucide.Ticket,
                            stringResource(Res.string.events_empty_title),
                            stringResource(Res.string.events_empty_body),
                            actionLabel = stringResource(Res.string.events_create),
                            onAction = { creating = true },
                        )
                        else -> Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                            list.forEachIndexed { index, event ->
                                val distance = here?.let { me ->
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
    }

    if (creating) {
        NewEventSheet(api, providers, onOpenProviders = { creating = false; onOpenProviders() }, onDismiss = { creating = false }) { id ->
            creating = false
            onOpenEvent(id)
        }
    }
}
