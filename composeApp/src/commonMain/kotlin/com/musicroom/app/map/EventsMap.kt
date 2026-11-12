package com.musicroom.app.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.Plus
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.map_zoom_in
import com.musicroom.app.generated.resources.map_zoom_out
import com.musicroom.app.theme.color.DarkColors
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.EventResponse
import com.musicroom.shared.dto.EventStatus
import com.musicroom.shared.dto.GeoPoint
import org.jetbrains.compose.resources.stringResource
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The events that gave a place, on an OpenStreetMap map that drags and pinches.
 *
 * Only the tiles the view covers are asked for, and the world repeats east-west so panning past the
 * date line keeps showing map. It opens on the user when their position is known, otherwise on a view
 * holding every event, otherwise on nothing in particular.
 */
@Composable
fun EventsMap(
    api: MusicRoomApi,
    events: List<EventResponse>,
    here: GeoPoint?,
    onOpenEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val tint = remember(colors) { MapTint.of(colors, dark = colors == DarkColors) }
    val placed = remember(events) { events.filter { it.location != null } }
    var camera by remember { mutableStateOf(MapCamera.covering(placed.mapNotNull { it.location }) ?: MapCamera.Default) }
    var followedUser by remember { mutableStateOf(false) }
    // The position arrives after the first frame: the map goes there once, then the user drives it.
    LaunchedEffect(here) {
        val point = here ?: return@LaunchedEffect
        if (followedUser) return@LaunchedEffect
        followedUser = true
        camera = MapCamera(point.latitude, point.longitude, MapCamera.NEARBY_ZOOM)
    }

    val density = LocalDensity.current
    val tileDp = MapTile.SIZE_DP.dp
    BoxWithConstraints(
        modifier.clipToBounds().background(colors.surfaceSubtle).pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                val size = with(density) { tileDp.toPx() } * camera.tileScale
                camera = camera.movedBy(pan.x, pan.y, size).zoomedBy(zoom)
            }
        },
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        // Draw the next level at half size: its extra pixels keep tiles sharp on dense screens.
        val zoom = camera.tileZoom + 1
        val tilePx = with(density) { tileDp.toPx() } * camera.tileScale / 2f
        val tiles = MapTile.count(zoom)
        // The view, in tiles: its top left corner is half a screen up and left of the centre.
        val left = MapTile.x(camera.longitude, zoom) - widthPx / 2f / tilePx
        val top = MapTile.y(camera.latitude, zoom) - heightPx / 2f / tilePx

        for (row in floor(top).toInt().coerceAtLeast(0)..floor(top + heightPx / tilePx).toInt().coerceAtMost(tiles - 1)) {
            for (column in floor(left).toInt()..floor(left + widthPx / tilePx).toInt()) {
                val wrapped = ((column % tiles) + tiles) % tiles
                key(zoom, wrapped, row) {
                    MapTileImage(
                        api,
                        MapTile(zoom, wrapped, row),
                        tint,
                        // Anchored by its middle, which lands its corner exactly where the grid wants it.
                        Modifier.anchored((column - left + 0.5f) * tilePx, (row - top + 0.5f) * tilePx, centred = true)
                            .size(with(density) { tilePx.toDp() }),
                    )
                }
            }
        }

        here?.let { point ->
            Box(
                Modifier.anchored((MapTile.x(point.longitude, zoom) - left) * tilePx, (MapTile.y(point.latitude, zoom) - top) * tilePx, centred = true)
                    .size(14.dp)
                    .background(colors.accentText, CircleShape)
                    .border(3.dp, colors.background, CircleShape),
            )
        }

        placed.forEach { event ->
            val point = event.location ?: return@forEach
            key(event.id) {
                EventMapPin(
                    event.name,
                    playing = event.status == EventStatus.PLAYING,
                    onClick = { onOpenEvent(event.id) },
                    modifier = Modifier.anchored(
                        (MapTile.x(point.longitude, zoom) - left) * tilePx,
                        (MapTile.y(point.latitude, zoom) - top) * tilePx,
                        centred = false,
                    ),
                )
            }
        }

        Column(
            Modifier.align(Alignment.BottomEnd).padding(AppTheme.spacing.sm),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
        ) {
            Column(
                Modifier.background(colors.surface, AppShapes.small).border(1.dp, colors.border, AppShapes.small),
            ) {
                IconButton(onClick = { camera = camera.zoomedBy(2f) }, modifier = Modifier.size(36.dp)) {
                    Icon(Lucide.Plus, contentDescription = stringResource(Res.string.map_zoom_in), tint = colors.textPrimary)
                }
                IconButton(onClick = { camera = camera.zoomedBy(0.5f) }, modifier = Modifier.size(36.dp)) {
                    Icon(Lucide.Minus, contentDescription = stringResource(Res.string.map_zoom_out), tint = colors.textPrimary)
                }
            }
            Text(
                MapTile.ATTRIBUTION,
                style = AppTheme.typography.label,
                color = colors.textSecondary,
                modifier = Modifier.background(colors.surface.copy(alpha = 0.8f), AppShapes.small).padding(horizontal = 4.dp),
            )
        }
    }
}

/** Places content at a point of the map: hanging from it by default, centred on it for a dot. */
private fun Modifier.anchored(x: Double, y: Double, centred: Boolean) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(placeable.width, placeable.height) {
        placeable.place(
            x.roundToInt() - placeable.width / 2,
            y.roundToInt() - if (centred) placeable.height / 2 else placeable.height,
        )
    }
}
