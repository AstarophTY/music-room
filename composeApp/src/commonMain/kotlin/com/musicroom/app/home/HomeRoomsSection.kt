package com.musicroom.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musicroom.app.common.UiText
import com.musicroom.app.common.WindowSize
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

/** Home shelf of the public rooms, each a square card; "See all" goes to the events tab. */
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

    val size: Dp = if (WindowSize.current != WindowSize.Compact) 192.dp else 164.dp
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
            else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                items(loaded.take(8), key = { it.id }) { event ->
                    HomeRoomCard(api, event, onClick = { onOpenRoom(event.id) }, size = size)
                }
            }
        }
    }
}
