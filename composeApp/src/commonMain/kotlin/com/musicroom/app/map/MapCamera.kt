package com.musicroom.app.map

import com.musicroom.shared.dto.GeoPoint
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow

/**
 * What the map shows: the point at the centre of the view, and how close [zoom] brings it.
 *
 * [zoom] is fractional so that a pinch moves smoothly, while tiles only exist at whole levels: the level
 * below is drawn, scaled up by [tileScale], until the next one is reached.
 */
data class MapCamera(val latitude: Double, val longitude: Double, val zoom: Float) {
    val tileZoom: Int get() = zoom.toInt().coerceIn(MIN_ZOOM, MAX_ZOOM)
    val tileScale: Float get() = 2f.pow(zoom - tileZoom)

    fun zoomedBy(factor: Float): MapCamera =
        copy(zoom = (zoom + log2(factor)).coerceIn(MIN_ZOOM.toFloat(), MAX_ZOOM.toFloat()))

    /** Drags the map by [dx] and [dy] screen pixels, where [tilePx] is the size a tile is drawn at. */
    fun movedBy(dx: Float, dy: Float, tilePx: Float): MapCamera {
        val zoom = tileZoom
        val x = MapTile.x(longitude, zoom) - dx / tilePx
        val y = MapTile.y(latitude, zoom) - dy / tilePx
        return copy(
            latitude = MapTile.latitude(y.coerceIn(0.0, MapTile.count(zoom).toDouble()), zoom),
            longitude = MapTile.longitude(x, zoom),
        )
    }

    companion object {
        const val MIN_ZOOM = 2
        const val MAX_ZOOM = 18

        /** Close enough to read a street, which is what "what is around me" means. */
        const val NEARBY_ZOOM = 13f

        /** Where the map opens when neither the user nor an event says otherwise. */
        val Default = MapCamera(48.8566, 2.3522, 4f)

        /** A view holding every point of [points], or null when there is none to hold. */
        fun covering(points: List<GeoPoint>): MapCamera? {
            if (points.isEmpty()) return null
            val latitudes = points.map { it.latitude }
            val longitudes = points.map { it.longitude }
            val centre = GeoPoint(
                latitude = (latitudes.min() + latitudes.max()) / 2,
                longitude = (longitudes.min() + longitudes.max()) / 2,
            )
            if (points.size == 1) return MapCamera(centre.latitude, centre.longitude, NEARBY_ZOOM)
            // The widest side decides: one level out doubles the degrees the view spans.
            val span = max(latitudes.max() - latitudes.min(), longitudes.max() - longitudes.min())
            val zoom = if (span <= 0.0) NEARBY_ZOOM else (log2(360.0 / span).toFloat() - 1f)
            return MapCamera(centre.latitude, centre.longitude, zoom.coerceIn(MIN_ZOOM.toFloat(), NEARBY_ZOOM))
        }
    }
}
