package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** Kind of album, as Spotify classes it. */
@Serializable
enum class AlbumType {
    ALBUM,
    SINGLE,
    COMPILATION,
}
