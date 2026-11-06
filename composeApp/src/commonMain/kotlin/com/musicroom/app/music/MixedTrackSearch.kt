package com.musicroom.app.music

import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TrackResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class MixedTrackSearch(private val api: MusicRoomApi) {
    data class Result(val tracks: List<TrackResponse>, val failures: List<Exception>)

    suspend fun search(query: String, spotify: Boolean, appleStorefront: String?): Result = fetch(
        spotify = spotify,
        appleStorefront = appleStorefront,
        spotifyRequest = { api.searchTracks(query).items },
        appleRequest = { api.searchAppleMusic(query, it).items },
    )

    suspend fun suggestions(spotify: Boolean, appleStorefront: String?): Result = fetch(
        spotify = spotify,
        appleStorefront = appleStorefront,
        spotifyRequest = { api.myTopTracks().items },
        appleRequest = { api.appleMusicCharts(it).items },
    )

    private suspend fun fetch(
        spotify: Boolean,
        appleStorefront: String?,
        spotifyRequest: suspend () -> List<TrackResponse>,
        appleRequest: suspend (String) -> List<TrackResponse>,
    ): Result = coroutineScope {
        suspend fun attempt(block: suspend () -> List<TrackResponse>): kotlin.Result<List<TrackResponse>> = try {
            kotlin.Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
        val spotifyTask = if (spotify) async { attempt { spotifyRequest() } } else null
        val appleTask = appleStorefront?.let { region -> async { attempt { appleRequest(region) } } }
        val spotifyResult = spotifyTask?.await()
        val appleResult = appleTask?.await()
        val first = spotifyResult?.getOrNull().orEmpty()
        val second = appleResult?.getOrNull().orEmpty()
        val mixed = buildList {
            for (index in 0 until maxOf(first.size, second.size)) {
                first.getOrNull(index)?.let(::add)
                second.getOrNull(index)?.let(::add)
            }
        }
        Result(mixed, listOfNotNull(spotifyResult?.exceptionOrNull(), appleResult?.exceptionOrNull()).filterIsInstance<Exception>())
    }
}
