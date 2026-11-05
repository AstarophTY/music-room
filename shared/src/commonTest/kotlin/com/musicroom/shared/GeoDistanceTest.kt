package com.musicroom.shared

import com.musicroom.shared.dto.CreateEventRequest
import com.musicroom.shared.dto.EventVoteLicense
import com.musicroom.shared.dto.GeoPoint
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeoDistanceTest {
    @Test
    fun knownDistances() {
        val paris = GeoPoint(48.8566, 2.3522)
        val lyon = GeoPoint(45.7640, 4.8357)
        // About 392 km.
        assertTrue(abs(GeoDistance.meters(paris, lyon) - 392_000) < 3_000)
        assertEquals(0.0, GeoDistance.meters(paris, paris))
        // 0.001 degree of latitude is about 111 m.
        assertTrue(abs(GeoDistance.meters(paris, paris.copy(latitude = paris.latitude + 0.001)) - 111) < 2)
    }

    @Test
    fun eventValidation() {
        val base = CreateEventRequest("Party")
        assertNull(EventValidation.error(base))
        assertEquals(ValidationError.EVENT_NAME_REQUIRED, EventValidation.error(base.copy(name = "  ")))
        assertEquals(ValidationError.EVENT_AREA_REQUIRED, EventValidation.error(base.copy(voteLicense = EventVoteLicense.LOCATION)))
        assertNull(
            EventValidation.error(base.copy(voteLicense = EventVoteLicense.LOCATION, location = GeoPoint(48.0, 2.0), radiusMeters = 200)),
        )
        assertEquals(ValidationError.EVENT_HOURS_INVALID, EventValidation.error(base.copy(startsAt = 10, endsAt = 5)))
    }
}
