package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val database: String,
)

@Serializable
data class InfoResponse(
    val name: String,
    val version: String,
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String? = null,
)
