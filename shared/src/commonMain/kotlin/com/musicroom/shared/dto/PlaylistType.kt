package com.musicroom.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Who can change a Music Room playlist. [PERSONAL]: only its owner adds songs, members can listen.
 * [SHARED]: members with [PlaylistAccess.WRITE] add and remove songs too. [key] is the value stored by the server.
 */
@Serializable
enum class PlaylistType(val key: String) {
    @SerialName("personal") PERSONAL("personal"),
    @SerialName("shared") SHARED("shared"),
    ;

    companion object {
        fun fromKey(key: String?): PlaylistType = entries.firstOrNull { it.key == key } ?: PERSONAL
    }
}
