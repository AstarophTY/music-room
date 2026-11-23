package com.musicroom.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Who can edit a playlist. [EVERYONE]: anyone who can open it. [INVITED]: its owner and the members with
 * [PlaylistAccess.WRITE]. [key] is the value stored by the server.
 */
@Serializable
enum class PlaylistEditLicense(val key: String) {
    @SerialName("everyone") EVERYONE("everyone"),
    @SerialName("invited") INVITED("invited"),
    ;

    companion object {
        /** An unknown value is the stricter license. */
        fun fromKey(key: String?): PlaylistEditLicense = entries.firstOrNull { it.key == key } ?: INVITED
    }
}
