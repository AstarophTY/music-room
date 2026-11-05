package com.musicroom.shared

/** Where the client keeps the refresh token between app launches. */
interface TokenStore {
    var refreshToken: String?
}
