package com.musicroom.server.common.security

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordHasherTest {
    private val hasher = PasswordHasher(cost = 4)

    @Test
    fun `hash is bcrypt and never the plain password`() = runBlocking {
        val hash = hasher.hash("hunter42abc")
        assertTrue(hash.startsWith("\$2a\$04\$"))
        assertFalse(hash.contains("hunter42abc"))
    }

    @Test
    fun `same password gives different hashes (salt)`() = runBlocking {
        assertNotEquals(hasher.hash("hunter42abc"), hasher.hash("hunter42abc"))
    }

    @Test
    fun `verify accepts the right password only`() = runBlocking {
        val hash = hasher.hash("hunter42abc")
        assertTrue(hasher.verify("hunter42abc", hash))
        assertFalse(hasher.verify("wrong-password1", hash))
    }

    @Test
    fun `verify rejects users without password`() = runBlocking {
        assertFalse(hasher.verify("hunter42abc", null))
    }
}
