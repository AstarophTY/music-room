package com.musicroom.shared

/** Profile detail rules shared by the app (instant feedback) and the server (source of truth). */
object ProfileValidation {
    const val INFO_MAX_LENGTH = 500
    const val MAX_PREFERENCES = 15
    const val PREFERENCE_MAX_LENGTH = 32

    /** Trimmed, without blanks or duplicates (whatever the case), in the order given. */
    fun cleanPreferences(values: List<String>): List<String> =
        values.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }

    fun isValid(infos: List<String>, preferences: List<String>): Boolean =
        infos.all { it.trim().length <= INFO_MAX_LENGTH } &&
            preferences.size <= MAX_PREFERENCES &&
            preferences.all { it.length <= PREFERENCE_MAX_LENGTH }
}
