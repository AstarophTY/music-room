package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** A position on Earth, in degrees (WGS 84, as phones give it). */
@Serializable
data class GeoPoint(val latitude: Double, val longitude: Double)
