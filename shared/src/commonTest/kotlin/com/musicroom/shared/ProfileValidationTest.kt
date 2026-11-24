package com.musicroom.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileValidationTest {
    @Test
    fun preferencesAreTrimmedAndUnique() {
        assertEquals(listOf("Jazz", "Rock"), ProfileValidation.cleanPreferences(listOf(" Jazz ", "", "jazz", "Rock")))
    }

    @Test
    fun limitsAreEnforced() {
        assertTrue(ProfileValidation.isValid(listOf("a".repeat(ProfileValidation.INFO_MAX_LENGTH)), listOf("Jazz")))
        assertFalse(ProfileValidation.isValid(listOf("a".repeat(ProfileValidation.INFO_MAX_LENGTH + 1)), emptyList()))
        assertFalse(ProfileValidation.isValid(emptyList(), List(ProfileValidation.MAX_PREFERENCES + 1) { "g$it" }))
        assertFalse(ProfileValidation.isValid(emptyList(), listOf("g".repeat(ProfileValidation.PREFERENCE_MAX_LENGTH + 1))))
    }
}
