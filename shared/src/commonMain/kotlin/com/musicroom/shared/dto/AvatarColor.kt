package com.musicroom.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Color of the initials avatar, used when the user has no photo. [key] is the value stored by the server. */
@Serializable
enum class AvatarColor(val key: String) {
    @SerialName("yellow") YELLOW("yellow"),
    @SerialName("tomato") TOMATO("tomato"),
    @SerialName("pink") PINK("pink"),
    @SerialName("iris") IRIS("iris"),
    @SerialName("teal") TEAL("teal"),
    @SerialName("grass") GRASS("grass"),
    ;

    companion object {
        fun fromKey(key: String?): AvatarColor = entries.firstOrNull { it.key == key } ?: YELLOW
    }
}
