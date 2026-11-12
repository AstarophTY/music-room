package com.musicroom.app.map

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.tan

/**
 * One square of the map at a zoom level, as OpenStreetMap serves them.
 *
 * The world is a single tile at zoom 0 and splits in four at every level, so a level holds 2^zoom tiles
 * each way: [x] counts them east from -180°, [y] south from the top of the Mercator square. The companion
 * converts between degrees and that grid, in fractions of a tile, which is what placing anything on the
 * map comes down to.
 */
data class MapTile(val zoom: Int, val x: Int, val y: Int) {
    val url: String get() = "https://tile.openstreetmap.org/$zoom/$x/$y.png"

    companion object {
        /** The size tiles are drawn at, in dp: their own 256 px would be a stamp on a dense screen. */
        const val SIZE_DP = 256

        /** Shown over the map: the tiles are not ours and OpenStreetMap asks for the credit. */
        const val ATTRIBUTION = "© OpenStreetMap"

        /**
         * Sent with every tile request: the OpenStreetMap tile policy turns away anything it cannot
         * identify, and the default of an HTTP library identifies nothing.
         */
        val HEADERS = mapOf("User-Agent" to "MusicRoom/0.1 (+https://github.com/AstarophTY/music-room)")

        /** Mercator stretches the poles to infinity: the square stops where the tiles do. */
        const val MAX_LATITUDE = 85.05112878

        fun count(zoom: Int): Int = 1 shl zoom

        fun x(longitude: Double, zoom: Int): Double = (longitude + 180.0) / 360.0 * count(zoom)

        fun y(latitude: Double, zoom: Int): Double {
            val radians = latitude.coerceIn(-MAX_LATITUDE, MAX_LATITUDE) * PI / 180.0
            return (1.0 - ln(tan(radians) + 1.0 / cos(radians)) / PI) / 2.0 * count(zoom)
        }

        fun longitude(x: Double, zoom: Int): Double = x / count(zoom) * 360.0 - 180.0

        fun latitude(y: Double, zoom: Int): Double {
            val n = PI - 2.0 * PI * y / count(zoom)
            return 180.0 / PI * atan(0.5 * (exp(n) - exp(-n)))
        }
    }
}
