package com.musicroom.shared

import com.musicroom.shared.dto.SocialNetwork

/** Turns what the user typed into a clean username, shared by the app and the server. */
object SocialLinks {
    /**
     * Accepts "name", "@name" or a profile link ("https://instagram.com/name?igsh=..."), returns "name".
     * Discord usernames are lowercase.
     */
    fun normalize(network: SocialNetwork, input: String): String {
        var value = input.trim()
        if (value.contains('/')) {
            value = value.substringBefore('?').substringBefore('#').trimEnd('/').substringAfterLast('/')
        }
        value = value.removePrefix("@")
        return if (network == SocialNetwork.DISCORD) value.lowercase() else value
    }

    fun handleError(network: SocialNetwork, handle: String): ValidationError? =
        if (network.handleRegex.matches(handle)) null else ValidationError.SOCIAL_HANDLE_INVALID
}
