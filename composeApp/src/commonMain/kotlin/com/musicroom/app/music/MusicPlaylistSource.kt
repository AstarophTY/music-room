package com.musicroom.app.music

import com.musicroom.shared.dto.TrackPageResponse

/** Playlists of one provider account. Implemented by providers that expose them. */
interface MusicPlaylistSource {
    /** The playlists saved in the linked account. */
    suspend fun playlists(): List<MusicPlaylist>

    /** One page of [playlist]'s tracks, starting at [offset]. */
    suspend fun tracks(playlist: MusicPlaylist, offset: Int): TrackPageResponse
}
