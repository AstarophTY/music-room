package com.musicroom.shared

import com.musicroom.shared.dto.GeoPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Distances on Earth, used for the area of an event (the server checks it, the app shows it). */
object GeoDistance {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /** Great-circle distance between [a] and [b], in meters (haversine formula). */
    fun meters(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = a.latitude.toRadians()
        val lat2 = b.latitude.toRadians()
        val dLat = lat2 - lat1
        val dLon = (b.longitude - a.longitude).toRadians()
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    fun isValid(point: GeoPoint): Boolean =
        point.latitude in -90.0..90.0 && point.longitude in -180.0..180.0 && !point.latitude.isNaN() && !point.longitude.isNaN()

    private fun Double.toRadians() = this * kotlin.math.PI / 180
}
