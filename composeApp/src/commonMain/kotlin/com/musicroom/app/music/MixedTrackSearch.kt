package com.musicroom.app.music

import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TrackResponse

class MixedTrackSearch(private val api: MusicRoomApi) {
    data class Result(val tracks: List<TrackResponse>, val failures: List<Exception>)

    suspend fun search(query: String, spotify: Boolean, appleStorefront: String?): Result {
        val result = MusicCatalogSearch.connected(api, spotify, appleStorefront).tracks(query)
        return Result(result.response?.tracks.orEmpty(), result.failures)
    }

    suspend fun suggestions(spotify: Boolean, appleStorefront: String?): Result {
        val result = MusicCatalogSearch.connected(api, spotify, appleStorefront).suggestions()
        return Result(result.response?.tracks.orEmpty(), result.failures)
    }
}
