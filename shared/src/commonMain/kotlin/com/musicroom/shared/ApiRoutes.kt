package com.musicroom.shared

object ApiRoutes {
    const val HEALTH = "/health"
    const val API_V1 = "/api/v1"
    const val INFO = "$API_V1/info"

    const val AUTH = "$API_V1/auth"
    const val AUTH_REGISTER = "$AUTH/register"
    const val AUTH_LOGIN = "$AUTH/login"
    const val AUTH_REFRESH = "$AUTH/refresh"
    const val AUTH_LOGOUT = "$AUTH/logout"
    const val AUTH_PASSWORD = "$AUTH/password"
    const val AUTH_PASSWORD_FORGOT = "$AUTH/password/forgot"
    const val AUTH_PASSWORD_RESET = "$AUTH/password/reset"
    const val AUTH_OAUTH = "$AUTH/oauth"
    const val AUTH_EMAIL_VERIFY = "$AUTH/email/verify"
    const val AUTH_EMAIL_RESEND = "$AUTH/email/resend"
    const val AUTH_2FA_VERIFY = "$AUTH/2fa/verify"
    const val AUTH_2FA_SETUP = "$AUTH/2fa/setup"
    const val AUTH_2FA_ENABLE = "$AUTH/2fa/enable"
    const val AUTH_2FA_DISABLE = "$AUTH/2fa/disable"
    const val AUTH_2FA_EMAIL_CODE = "$AUTH/2fa/email/code"
    const val AUTH_2FA_EMAIL_ENABLE = "$AUTH/2fa/email/enable"
    const val AUTH_2FA_EMAIL_RESEND = "$AUTH/2fa/email/resend"

    const val SPOTIFY = "$API_V1/spotify"
    const val SPOTIFY_STATUS = "$SPOTIFY/status"
    const val SPOTIFY_CONNECT = "$SPOTIFY/connect"
    const val SPOTIFY_CALLBACK = "$SPOTIFY/callback"

    const val MUSIC = "$API_V1/music"
    const val MUSIC_SEARCH = "$MUSIC/search"
    const val MUSIC_TRACK = "$MUSIC/tracks/{id}"
    const val MUSIC_MY_PLAYLISTS = "$MUSIC/me/playlists"
    const val MUSIC_MY_TOP_TRACKS = "$MUSIC/me/top-tracks"
    const val MUSIC_PLAYLIST_TRACKS = "$MUSIC/playlists/{id}/tracks"
    const val MUSIC_RECOMMENDATIONS = "$MUSIC/me/recommendations"
    const val MUSIC_PLAYER = "$MUSIC/player"
    const val MUSIC_PLAYER_PLAY = "$MUSIC_PLAYER/play"
    const val MUSIC_PLAYER_PAUSE = "$MUSIC_PLAYER/pause"
    const val MUSIC_PLAYER_NEXT = "$MUSIC_PLAYER/next"
    const val MUSIC_PLAYER_PREVIOUS = "$MUSIC_PLAYER/previous"
    const val MUSIC_PLAYER_SEEK = "$MUSIC_PLAYER/seek"
    const val MUSIC_PLAYER_QUEUE = "$MUSIC_PLAYER/queue"

    fun track(id: String) = "$MUSIC/tracks/$id"

    fun playlistTracks(id: String) = "$MUSIC/playlists/$id/tracks"

    /** Playlists made in Music Room (the MUSIC_* playlist routes are the user's Spotify ones). */
    const val PLAYLISTS = "$API_V1/playlists"
    const val PLAYLIST = "$PLAYLISTS/{id}"
    const val PLAYLIST_COVER = "$PLAYLIST/cover"
    const val PLAYLIST_TRACKS = "$PLAYLIST/tracks"
    const val PLAYLIST_TRACK = "$PLAYLIST/tracks/{entryId}"
    const val PLAYLIST_MEMBERS = "$PLAYLIST/members"
    const val PLAYLIST_MEMBER = "$PLAYLIST/members/{userId}"

    fun playlist(id: String) = "$PLAYLISTS/$id"

    fun playlistCover(id: String) = "$PLAYLISTS/$id/cover"

    fun roomPlaylistTracks(id: String) = "$PLAYLISTS/$id/tracks"

    fun roomPlaylistTrack(id: String, entryId: String) = "$PLAYLISTS/$id/tracks/$entryId"

    fun playlistMembers(id: String) = "$PLAYLISTS/$id/members"

    fun playlistMember(id: String, userId: String) = "$PLAYLISTS/$id/members/$userId"

    const val USERS = "$API_V1/users"
    const val USERS_ME = "$USERS/me"
    const val USERS_ME_AVATAR = "$USERS_ME/avatar"
    const val USERS_ME_SOCIAL_LINKS = "$USERS_ME/social-links"
    const val USER_AVATAR = "$USERS/{id}/avatar"

    fun userAvatar(userId: String) = "$USERS/$userId/avatar"
}
