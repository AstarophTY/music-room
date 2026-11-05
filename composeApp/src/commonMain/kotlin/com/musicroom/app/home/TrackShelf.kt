package com.musicroom.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.musicroom.app.common.WindowSize
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_next
import com.musicroom.app.generated.resources.common_previous
import com.musicroom.app.music.TrackCard
import com.musicroom.app.player.PlayerController
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TrackResponse
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * One titled row of recommended tracks. Tapping a track plays the row as the queue.
 * Phones swipe the row; larger screens get bigger covers and arrows that page through it.
 */
@Composable
fun TrackShelf(title: String, tracks: List<TrackResponse>, api: MusicRoomApi, player: PlayerController) {
    val colors = AppTheme.colors
    val wide = WindowSize.current != WindowSize.Compact
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll by the number of cards on screen, so each click shows a new page.
    fun page(forward: Boolean) {
        val visible = state.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
        val target = state.firstVisibleItemIndex + if (forward) visible - 1 else -(visible - 1)
        scope.launch { state.animateScrollToItem(target.coerceIn(0, tracks.lastIndex.coerceAtLeast(0))) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = AppTheme.typography.label, color = colors.textSecondary)
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    ShelfArrow(Lucide.ChevronLeft, stringResource(Res.string.common_previous), state.canScrollBackward) { page(false) }
                    ShelfArrow(Lucide.ChevronRight, stringResource(Res.string.common_next), state.canScrollForward) { page(true) }
                }
            }
        }
        LazyRow(state = state, horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                TrackCard(
                    api,
                    track,
                    playing = player.track?.id == track.id,
                    onClick = { player.play(tracks, index) },
                    size = if (wide) 168.dp else 148.dp,
                )
            }
        }
    }
}
