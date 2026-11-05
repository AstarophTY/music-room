package com.musicroom.shared

import com.musicroom.shared.dto.CreateEventRequest
import com.musicroom.shared.dto.EventVoteLicense

/** Rules for an event's settings, checked by the app before sending and by the server. */
object EventValidation {
    const val NAME_MAX_LENGTH = 64
    const val DESCRIPTION_MAX_LENGTH = 280
    const val MIN_RADIUS_METERS = 50
    const val MAX_RADIUS_METERS = 50_000

    fun nameError(name: String): ValidationError? = when {
        name.isBlank() -> ValidationError.EVENT_NAME_REQUIRED
        name.trim().length > NAME_MAX_LENGTH -> ValidationError.EVENT_NAME_TOO_LONG
        else -> null
    }

    /** The first problem of [request], or null. */
    fun error(request: CreateEventRequest): ValidationError? {
        nameError(request.name)?.let { return it }
        if (request.description.trim().length > DESCRIPTION_MAX_LENGTH) return ValidationError.EVENT_DESCRIPTION_TOO_LONG
        val start = request.startsAt
        val end = request.endsAt
        if (start != null && end != null && end <= start) return ValidationError.EVENT_HOURS_INVALID
        if (request.voteLicense == EventVoteLicense.LOCATION) {
            val location = request.location
            val radius = request.radiusMeters
            if (location == null || !GeoDistance.isValid(location) || radius == null) return ValidationError.EVENT_AREA_REQUIRED
            if (radius !in MIN_RADIUS_METERS..MAX_RADIUS_METERS) return ValidationError.EVENT_AREA_REQUIRED
        }
        return null
    }
}
