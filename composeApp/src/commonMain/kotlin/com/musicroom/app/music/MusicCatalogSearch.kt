package com.musicroom.app.music

import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.SearchResponse
import com.musicroom.shared.dto.TrackResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Searches enabled catalogs concurrently and keeps successful results when one provider fails. */
class MusicCatalogSearch(private val providers: List<Provider>) {
    interface Provider {
        suspend fun search(query: String, mood: String?): SearchResponse
        suspend fun tracks(query: String): List<TrackResponse> = search(query, null).tracks
        suspend fun suggestions(): List<TrackResponse>
    }

    data class Result(val response: SearchResponse?, val failures: List<Exception>)

    suspend fun search(query: String, mood: String? = null): Result {
        val results = fetch { search(query, mood) }
        val successful = results.mapNotNull { it.getOrNull() }
        val tracks = successful.map { it.tracks }
        return Result(
            response = if (successful.isEmpty()) null else SearchResponse(
                tracks = interleave(tracks),
                artists = successful.flatMap { it.artists },
                albums = successful.flatMap { it.albums },
                nextTrackOffset = successful.firstNotNullOfOrNull { it.nextTrackOffset },
            ),
            failures = results.mapNotNull { it.exceptionOrNull() as? Exception },
        )
    }

    suspend fun tracks(query: String): Result = collectTracks { tracks(query) }

    suspend fun suggestions(): Result = collectTracks { suggestions() }

    private suspend fun collectTracks(request: suspend Provider.() -> List<TrackResponse>): Result {
        val results = fetch { SearchResponse(tracks = request()) }
        val successful = results.mapNotNull { it.getOrNull() }
        return Result(
            if (successful.isEmpty()) null else SearchResponse(tracks = interleave(successful.map { it.tracks })),
            results.mapNotNull { it.exceptionOrNull() as? Exception },
        )
    }

    private suspend fun fetch(request: suspend Provider.() -> SearchResponse) = coroutineScope {
        providers.map { provider -> async {
            try { kotlin.Result.success(provider.request()) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { kotlin.Result.failure(e) }
        } }.awaitAll()
    }

    private fun interleave(lists: List<List<TrackResponse>>): List<TrackResponse> = buildList {
        for (index in 0 until (lists.maxOfOrNull { it.size } ?: 0)) {
            lists.forEach { it.getOrNull(index)?.let(::add) }
        }
    }

    companion object {
        fun connected(api: MusicRoomApi, spotify: Boolean, appleStorefront: String?): MusicCatalogSearch =
            MusicCatalogSearch(buildList {
                if (spotify) add(Spotify(api))
                if (appleStorefront != null) add(AppleMusic(api, appleStorefront))
            })
    }

    private class Spotify(private val api: MusicRoomApi) : Provider {
        override suspend fun search(query: String, mood: String?) = api.search(query)
        override suspend fun tracks(query: String) = api.searchTracks(query).items
        override suspend fun suggestions() = api.myTopTracks().items
    }

    private class AppleMusic(private val api: MusicRoomApi, private val storefront: String) : Provider {
        override suspend fun search(query: String, mood: String?) = api.searchAppleMusic(mood ?: query, storefront)
        override suspend fun suggestions() = api.appleMusicCharts(storefront).items
    }
}
