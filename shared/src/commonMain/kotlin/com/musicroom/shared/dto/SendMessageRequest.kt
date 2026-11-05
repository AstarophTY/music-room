package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequest(val body: String)
