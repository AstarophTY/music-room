package com.musicroom.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Rights of an invited member. [READ]: sees and plays the playlist. [WRITE]: also edits it when its license is
 * [PlaylistEditLicense.INVITED]. [key] is the value stored by the server.
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
