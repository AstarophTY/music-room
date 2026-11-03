package com.musicroom.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AuthValidationTest {
    @Test
    fun emailIsNormalized() {
        assertEquals("alice@example.com", AuthValidation.normalizeEmail("  Alice@Example.COM "))
    }

    @Test
    fun emailFormat() {
        assertNull(AuthValidation.emailError("alice@example.com"))
        assertNotNull(AuthValidation.emailError(""))
        assertNotNull(AuthValidation.emailError("alice"))
        assertNotNull(AuthValidation.emailError("alice@example"))
        assertNotNull(AuthValidation.emailError("ali ce@example.com"))
    }

    @Test
    fun passwordRules() {
        assertNull(AuthValidation.passwordError("hunter42abc"))
        assertNotNull(AuthValidation.passwordError("short1"))
        assertNotNull(AuthValidation.passwordError("onlyletters"))
        assertNotNull(AuthValidation.passwordError("12345678"))
        assertNotNull(AuthValidation.passwordError("a1" + "x".repeat(71)))
    }

    @Test
    fun displayNameRules() {
        assertNull(AuthValidation.displayNameError("Alice"))
        assertNotNull(AuthValidation.displayNameError("   "))
        assertNotNull(AuthValidation.displayNameError("x".repeat(65)))
    }

    @Test
    fun passwordConfirmation() {
        assertNull(AuthValidation.passwordConfirmationError("hunter42abc", "hunter42abc"))
        assertNotNull(AuthValidation.passwordConfirmationError("hunter42abc", ""))
        assertNotNull(AuthValidation.passwordConfirmationError("hunter42abc", "hunter42abd"))
    }

    @Test
    fun termsMustBeAccepted() {
        assertNull(AuthValidation.termsError(true))
        assertNotNull(AuthValidation.termsError(false))
    }

    @Test
    fun verificationCodeFormat() {
        assertEquals("123456", AuthValidation.normalizeVerificationCode(" 123 456 "))
        assertNull(AuthValidation.verificationCodeError("123456"))
        assertNotNull(AuthValidation.verificationCodeError("12345"))
        assertNotNull(AuthValidation.verificationCodeError("1234567"))
        assertNotNull(AuthValidation.verificationCodeError("12a456"))
    }
}
