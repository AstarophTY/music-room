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
    const val APPLE_MUSIC_DEVELOPER_TOKEN = "$API_V1/apple-music/developer-token"
    const val APPLE_MUSIC_ARTIST = "$API_V1/apple-music/artist"
    const val APPLE_MUSIC_ALBUM = "$API_V1/apple-music/album"
    const val APPLE_MUSIC_SEARCH = "$API_V1/apple-music/search"
    const val APPLE_MUSIC_CHARTS = "$API_V1/apple-music/charts"
    const val APPLE_MUSIC_MATCH = "$API_V1/apple-music/match"

    const val MUSIC = "$API_V1/music"
    const val MUSIC_SEARCH = "$MUSIC/search"
    const val MUSIC_SEARCH_ALL = "$MUSIC/search/all"
    const val MUSIC_ARTIST = "$MUSIC/artists/{id}"
    const val MUSIC_ALBUM = "$MUSIC/albums/{id}"
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
    const val MUSIC_PLAYER_DEVICES = "$MUSIC_PLAYER/devices"
    const val MUSIC_PLAYER_DEVICE = "$MUSIC_PLAYER/device"

    fun track(id: String) = "$MUSIC/tracks/$id"

    fun playlistTracks(id: String) = "$MUSIC/playlists/$id/tracks"

    fun artist(id: String) = "$MUSIC/artists/$id"

    fun album(id: String) = "$MUSIC/albums/$id"

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

    const val USERS_SEARCH = "$USERS/search"
    const val USER_PROFILE = "$USERS/{id}"

    fun userProfile(userId: String) = "$USERS/$userId"

    const val USER_TRACKS = "$USERS/{id}/tracks"

    fun userTracks(userId: String) = "$USERS/$userId/tracks"

    /** Music Room Premium, paid with Stripe. */
    const val BILLING = "$API_V1/billing"
    const val BILLING_CHECKOUT = "$BILLING/checkout"
    const val BILLING_PORTAL = "$BILLING/portal"

    /** Called by Stripe (signed), not by the app. */
    const val BILLING_WEBHOOK = "$BILLING/webhook"

    /** Page shown in the browser after paying or leaving the Stripe page. */
    const val BILLING_RETURN = "$BILLING/return"

    /** Songs uploaded to Music Room (Premium): the user's own list, and the upload. */
    const val HOSTED_TRACKS = "$API_V1/tracks"
    const val HOSTED_TRACK = "$HOSTED_TRACKS/{id}"
    const val HOSTED_TRACK_AUDIO = "$HOSTED_TRACK/audio"

    fun hostedTrack(id: String) = "$HOSTED_TRACKS/$id"

    fun hostedTrackAudio(id: String) = "$HOSTED_TRACKS/$id/audio"

    const val FRIENDS = "$API_V1/friends"
    const val FRIEND = "$FRIENDS/{userId}"

    fun friend(userId: String) = "$FRIENDS/$userId"

    const val CONVERSATIONS = "$API_V1/conversations"
    const val CONVERSATION_MESSAGES = "$CONVERSATIONS/{userId}/messages"
    const val CONVERSATION_READ = "$CONVERSATIONS/{userId}/read"

    fun conversationMessages(userId: String) = "$CONVERSATIONS/$userId/messages"

    fun conversationRead(userId: String) = "$CONVERSATIONS/$userId/read"

    const val NOTIFICATIONS = "$API_V1/notifications"
    const val NOTIFICATIONS_READ = "$NOTIFICATIONS/read"
    const val SOCIAL_COUNTS = "$API_V1/social/counts"

    /** WebSocket of live social events; the access token goes in the "token" query parameter. */
    const val SOCIAL_WS = "$API_V1/social/ws"

    /** Events: rooms where people propose songs and vote; the owner's Spotify plays the most voted one. */
    const val EVENTS = "$API_V1/events"
    const val EVENT = "$EVENTS/{id}"
    const val EVENT_TRACKS = "$EVENT/tracks"
    const val EVENT_TRACK = "$EVENT_TRACKS/{trackId}"
    const val EVENT_TRACK_VOTE = "$EVENT_TRACK/vote"
    const val EVENT_INVITED = "$EVENT/invited/{userId}"
    const val EVENT_PLAY = "$EVENT/play"
    const val EVENT_PAUSE = "$EVENT/pause"
    const val EVENT_SKIP = "$EVENT/skip"
    const val EVENT_STOP = "$EVENT/stop"

    /** WebSocket of an event's state; the access token goes in the "token" query parameter. */
    const val EVENT_WS = "$EVENT/ws"

    fun event(id: String) = "$EVENTS/$id"

    fun eventTracks(id: String) = "$EVENTS/$id/tracks"

    fun eventTrack(id: String, trackId: String) = "$EVENTS/$id/tracks/$trackId"

    fun eventTrackVote(id: String, trackId: String) = "$EVENTS/$id/tracks/$trackId/vote"

    fun eventInvited(id: String, userId: String) = "$EVENTS/$id/invited/$userId"

    fun eventPlay(id: String) = "$EVENTS/$id/play"

    fun eventPause(id: String) = "$EVENTS/$id/pause"

    fun eventSkip(id: String) = "$EVENTS/$id/skip"

    fun eventStop(id: String) = "$EVENTS/$id/stop"

    fun eventWs(id: String) = "$EVENTS/$id/ws"
}
