package com.musicroom.server.modules.auth.entities

import java.util.UUID

data class EmailVerificationCode(
    val id: UUID,
    val codeHash: String,
)
