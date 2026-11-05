package com.musicroom.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class OAuthProvider(val displayName: String) {
    @SerialName("google")
    GOOGLE("Google"),

    @SerialName("apple")
    APPLE("Apple"),
}
