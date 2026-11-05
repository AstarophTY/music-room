package com.musicroom.server.modules.spotify.client.model

import com.musicroom.shared.dto.DeviceResponse
import com.musicroom.shared.dto.DeviceType
import kotlinx.serialization.Serializable

/** A device where Spotify can play (phone, computer, speaker). [id] is null for some restricted devices. */
@Serializable
data class SpotifyDevice(
    val id: String? = null,
    val isActive: Boolean = false,
    val isRestricted: Boolean = false,
    val name: String = "",
    val type: String = "",
) {
    /** Null for devices the app cannot control (no id, or restricted). */
    fun toResponse(): DeviceResponse? {
        if (id == null || isRestricted) return null
        val kind = when (type.lowercase()) {
            "computer" -> DeviceType.COMPUTER
            "smartphone" -> DeviceType.SMARTPHONE
            "tablet" -> DeviceType.TABLET
            "speaker", "avr", "stb", "audiodongle", "castaudio" -> DeviceType.SPEAKER
            "tv", "castvideo", "gameconsole" -> DeviceType.TV
            "automobile" -> DeviceType.CAR
            else -> DeviceType.OTHER
        }
        return DeviceResponse(id, name, kind, isActive)
    }
}
