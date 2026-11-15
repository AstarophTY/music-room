package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** The Apple Music region ("fr", "us") the signed-in account browses, needed to query the catalog. */
@Serializable
data class AppleMusicStorefront(val storefront: String)
