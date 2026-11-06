package com.musicroom.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.musicroom.app.common.Reveal
import com.musicroom.app.common.UiText
import com.musicroom.app.events.EventCard
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.common_retry
import com.musicroom.app.generated.resources.home_rooms_empty
import com.musicroom.app.generated.resources.home_rooms_title
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.EventResponse
import com.musicroom.shared.dto.EventsScope
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.stringResource

/** Home shortcut to the public rooms open now, with "See all" going to the events tab. */
@Composable
fun HomeRoomsSection(api: MusicRoomApi, onOpenRoom: (String) -> Unit, onSeeAll: () -> Unit) {
    val colors = AppTheme.colors
    var events by remember { mutableStateOf<List<EventResponse>?>(null) }
    var error by remember { mutableStateOf<UiText?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        error = null
        try {
            events = api.events(EventsScope.PUBLIC).items
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
        HomeSectionHeader(stringResource(Res.string.home_rooms_title), onSeeAll)
        val loaded = events
        val failure = error
        when {
            failure != null -> {
                Text(failure.resolve(), style = AppTheme.typography.bodySmall, color = colors.error)
                AppButton(stringResource(Res.string.common_retry), { attempt++ }, variant = AppButtonVariant.Secondary)
            }
            loaded == null -> Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            loaded.isEmpty() -> Text(stringResource(Res.string.home_rooms_empty), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            // The events tab lists them all.
            else -> loaded.take(3).forEachIndexed { index, event ->
                Reveal(index) { EventCard(api, event, onClick = { onOpenRoom(event.id) }) }
            }
        }
    }
}
