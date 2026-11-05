package com.musicroom.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Rights of a member on a playlist. [READ]: sees and plays it. [WRITE]: also adds and removes songs and
 * changes the photo, only in a [PlaylistType.SHARED] playlist. [key] is the value stored by the server.
 */
@Serializable
enum class PlaylistAccess(val key: String) {
    @SerialName("read") READ("read"),
    @SerialName("write") WRITE("write"),
    ;

    companion object {
        fun fromKey(key: String?): PlaylistAccess = entries.firstOrNull { it.key == key } ?: READ
    }
}
