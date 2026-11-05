package com.musicroom.server.modules.spotify

/** What happened when Spotify sent the user back to the server (shown on the callback page). */
enum class SpotifyCallbackOutcome {
    Connected,

    /** The user clicked "Cancel" on the Spotify page. */
    Denied,

    /** The link was too old or not made by this server (state check failed). */
    Expired,

    /** The Spotify app is in development mode and this account is not in its user list. */
    NotAllowed,
    Failed,
}
