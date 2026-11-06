package com.musicroom.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Disc3
import com.composables.icons.lucide.Lucide
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.event_by
import com.musicroom.app.generated.resources.event_live
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.EventResponse
import com.musicroom.shared.dto.EventStatus
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.stringResource

private val roomCoverCache = mutableMapOf<String, String>()

/** Square room card of the home page: what plays (a disc otherwise), its name and who opened the room. */
@Composable
fun HomeRoomCard(
    api: MusicRoomApi,
    event: EventResponse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 164.dp,
) {
    val colors = AppTheme.colors
    var roomCover by remember(event.id) {
        mutableStateOf(event.nowPlaying?.imageUrl ?: roomCoverCache[event.id])
    }

    LaunchedEffect(event.id, event.nowPlaying?.imageUrl) {
        if (event.nowPlaying?.imageUrl != null) {
            roomCover = event.nowPlaying?.imageUrl
            return@LaunchedEffect
        }
        if (roomCover != null) return@LaunchedEffect
        try {
            val detail = api.event(event.id)
            val cover = detail.nowPlaying?.track?.imageUrl
                ?: detail.next?.track?.imageUrl
                ?: detail.queue.firstOrNull()?.track?.imageUrl
            if (cover != null) {
                roomCoverCache[event.id] = cover
                roomCover = cover
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    Column(
        modifier.width(size).pressable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        Box(Modifier.size(size).clip(AppShapes.large).background(colors.surface)) {
            val cover = roomCover
            if (cover != null) {
                RemoteImage(api, cover, Modifier.matchParentSize())
            } else {
                Box(
                    Modifier.matchParentSize()
                        .background(Brush.linearGradient(listOf(colors.accent, colors.accentSubtleStrong))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Lucide.Disc3, contentDescription = null, tint = colors.onAccent, modifier = Modifier.fillMaxSize(0.38f))
                }
            }
            if (event.status == EventStatus.PLAYING) {
                Text(
                    stringResource(Res.string.event_live),
                    modifier = Modifier.align(Alignment.TopStart).padding(AppTheme.spacing.sm)
                        .background(colors.accent, AppShapes.pill)
                        .padding(horizontal = AppTheme.spacing.sm, vertical = 2.dp),
                    style = AppTheme.typography.bodySmall,
                    color = colors.onAccent,
                )
            }
        }
        Text(
            event.name,
            style = AppTheme.typography.label,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            stringResource(Res.string.event_by, event.owner.displayName),
            style = AppTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
