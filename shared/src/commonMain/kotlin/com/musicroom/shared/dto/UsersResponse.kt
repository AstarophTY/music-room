package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class UsersResponse(val items: List<PublicUserResponse> = emptyList())
