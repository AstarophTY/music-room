package com.musicroom.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Who can find and open a playlist. [PRIVATE]: its owner and invited members. [key] is the value stored by the server. */
@Serializable
enum class PlaylistVisibility(val key: String) {
    @SerialName("public") PUBLIC("public"),
    @SerialName("private") PRIVATE("private"),
    ;

    companion object {
        /** An unknown value stays private, so a playlist is never exposed by mistake. */
        fun fromKey(key: String?): PlaylistVisibility = entries.firstOrNull { it.key == key } ?: PRIVATE
    }
}
