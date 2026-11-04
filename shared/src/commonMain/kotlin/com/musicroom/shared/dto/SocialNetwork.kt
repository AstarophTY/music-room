package com.musicroom.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Networks a user can show on their profile. [profileUrl] builds the public page from the username
 * (null for Discord: usernames have no public page). [handleRegex] is the username format of the network.
 */
@Serializable
enum class SocialNetwork(
    val key: String,
    val displayName: String,
    private val profileUrl: String?,
    val handleRegex: Regex,
) {
    @SerialName("instagram")
    INSTAGRAM("instagram", "Instagram", "https://instagram.com/%s", Regex("^[A-Za-z0-9._]{1,30}$")),

    @SerialName("tiktok")
    TIKTOK("tiktok", "TikTok", "https://www.tiktok.com/@%s", Regex("^[A-Za-z0-9._]{2,24}$")),

    @SerialName("x")
    X("x", "X", "https://x.com/%s", Regex("^[A-Za-z0-9_]{1,15}$")),

    @SerialName("youtube")
    YOUTUBE("youtube", "YouTube", "https://www.youtube.com/@%s", Regex("^[A-Za-z0-9._-]{3,30}$")),

    @SerialName("twitch")
    TWITCH("twitch", "Twitch", "https://www.twitch.tv/%s", Regex("^[A-Za-z0-9_]{4,25}$")),

    @SerialName("discord")
    DISCORD("discord", "Discord", null, Regex("^[a-z0-9._]{2,32}$")),

    @SerialName("spotify")
    SPOTIFY("spotify", "Spotify", "https://open.spotify.com/user/%s", Regex("^[A-Za-z0-9._-]{1,64}$")),

    @SerialName("soundcloud")
    SOUNDCLOUD("soundcloud", "SoundCloud", "https://soundcloud.com/%s", Regex("^[a-z0-9_-]{3,25}$")),
    ;

    fun urlFor(handle: String): String? = profileUrl?.replace("%s", handle)

    companion object {
        fun fromKey(key: String): SocialNetwork? = entries.firstOrNull { it.key == key }
    }
}
