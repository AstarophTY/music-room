package com.musicroom.shared

/** Error returned by the backend (non-2xx response). */
class ApiException(
    val status: Int,
    val error: String,
    message: String? = null,
) : RuntimeException(message ?: error)
