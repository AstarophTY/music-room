package com.musicroom.server.common.security

import at.favre.lib.crypto.bcrypt.BCrypt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** bcrypt password hashing. Hashing is slow on purpose, so it runs off the request threads. */
class PasswordHasher(private val cost: Int = DEFAULT_COST) {

    /** Compared against when the user does not exist, so a login takes the same time either way. */
    private val dummyHash: String by lazy { BCrypt.withDefaults().hashToString(cost, "dummy-password".toCharArray()) }

    suspend fun hash(password: String): String = withContext(Dispatchers.Default) {
        BCrypt.withDefaults().hashToString(cost, password.toCharArray())
    }

    suspend fun verify(password: String, hash: String?): Boolean = withContext(Dispatchers.Default) {
        val result = BCrypt.verifyer().verify(password.toCharArray(), hash ?: dummyHash)
        hash != null && result.verified
    }

    companion object {
        const val DEFAULT_COST = 12
    }
}
