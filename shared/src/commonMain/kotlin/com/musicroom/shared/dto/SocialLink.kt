package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** A username on a social network, shown on the profile. */
@Serializable
data class SocialLink(
    val network: SocialNetwork,
    val handle: String,
)
