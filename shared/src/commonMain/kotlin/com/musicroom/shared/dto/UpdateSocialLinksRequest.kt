package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Replaces all the user's social links (one per network at most). */
@Serializable
data class UpdateSocialLinksRequest(
    val links: List<SocialLink>,
)
