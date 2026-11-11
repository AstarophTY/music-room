package com.musicroom.server.modules.applemusic

import com.musicroom.server.common.exceptions.BadRequestException
import com.musicroom.server.common.exceptions.HttpException
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.shared.dto.AlbumDetailResponse
import com.musicroom.shared.dto.ArtistDetailResponse
import com.musicroom.shared.dto.AlbumResponse
import com.musicroom.shared.dto.AlbumType
import com.musicroom.shared.dto.ArtistResponse
import com.musicroom.shared.dto.SearchResponse
import com.musicroom.shared.dto.TrackPageResponse
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TrackSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class AppleMusicCatalog(private val tokens: AppleMusicTokenService) {
    private val client = HttpClient(CIO)

    private suspend fun get(url: String, block: HttpRequestBuilder.() -> Unit): HttpResponse = try {
        client.get(url, block)
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        throw e
    } catch (e: Exception) {
        throw HttpException(HttpStatusCode.BadGateway, "apple_music_upstream_error", "Apple Music catalog is unavailable")
    }

    suspend fun search(query: String, storefront: String): SearchResponse {
        val region = validateStorefront(storefront)
        if (query.isBlank() || query.length > 200) throw BadRequestException("Invalid search query")
        val response = get("https://api.music.apple.com/v1/catalog/$region/search") {
            bearerAuth(tokens.create())
            parameter("term", query)
            parameter("types", "songs,albums,artists")
            parameter("limit", 25)
        }
        if (!response.status.isSuccess()) throw HttpException(HttpStatusCode.BadGateway, "apple_music_upstream_error", "Apple Music catalog request failed")
        val root = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val results = root["results"]?.jsonObject
        val songs = results?.get("songs")?.jsonObject?.get("data")?.jsonArray.orEmpty()
        val albums = results?.get("albums")?.jsonObject?.get("data")?.jsonArray.orEmpty()
        val artists = results?.get("artists")?.jsonObject?.get("data")?.jsonArray.orEmpty()
        return SearchResponse(
            tracks = songs.map { song(it.jsonObject, region) },
            artists = artists.map { artist(it.jsonObject, region) },
            albums = albums.map { album(it.jsonObject, region) },
        )
    }

    private fun artist(item: JsonObject, storefront: String): ArtistResponse {
        val id = item.getValue("id").jsonPrimitive.content
        val attributes = item.getValue("attributes").jsonObject
        fun value(key: String) = attributes[key]?.jsonPrimitive?.contentOrNull.orEmpty()
        val image = attributes["artwork"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
            ?.replace("{w}", "300")?.replace("{h}", "300")
        return ArtistResponse(
            id = "apple:$storefront:$id",
            name = value("name"),
            imageUrl = image,
            uri = id,
        )
    }

    private fun album(item: JsonObject, storefront: String): AlbumResponse {
        val id = item.getValue("id").jsonPrimitive.content
        val attributes = item.getValue("attributes").jsonObject
        fun value(key: String) = attributes[key]?.jsonPrimitive?.contentOrNull.orEmpty()
        val image = attributes["artwork"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
            ?.replace("{w}", "300")?.replace("{h}", "300")
        return AlbumResponse(
            id = "apple:$storefront:$id",
            name = value("name"),
            artists = relationship(item, "artists").map { artist(it.jsonObject, storefront) },
            imageUrl = image,
            releaseYear = value("releaseDate").take(4).takeIf { it.isNotBlank() },
            trackCount = attributes["trackCount"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            type = AlbumType.ALBUM,
            uri = id,
        )
    }

    private suspend fun catalogItem(identifier: String, type: String, include: String): Pair<JsonObject, String> {
        val parts = identifier.split(':')
        if (parts.size != 3 || parts[0] != "apple" || !parts[2].matches(Regex("[0-9]{1,32}"))) {
            throw BadRequestException("Invalid Apple Music identifier")
        }
        val region = validateStorefront(parts[1])
        val response = get("https://api.music.apple.com/v1/catalog/$region/$type/${parts[2]}") {
            bearerAuth(tokens.create())
            parameter("include", include)
        }
        if (response.status == HttpStatusCode.NotFound) throw NotFoundException("Apple Music item not found")
        if (!response.status.isSuccess()) throw HttpException(HttpStatusCode.BadGateway, "apple_music_upstream_error", "Apple Music catalog request failed")
        val data = Json.parseToJsonElement(response.bodyAsText()).jsonObject["data"]?.jsonArray
        return (data?.firstOrNull()?.jsonObject ?: throw NotFoundException("Apple Music item not found")) to region
    }

    private fun relationship(item: JsonObject, name: String) =
        item["relationships"]?.jsonObject?.get(name)?.jsonObject?.get("data")?.jsonArray.orEmpty()

    suspend fun artistDetail(identifier: String): ArtistDetailResponse {
        val (item, region) = catalogItem(identifier, "artists", "albums")
        val artist = artist(item, region)
        val albums = relationship(item, "albums").map { album(it.jsonObject, region) }
        val tracks = search(artist.name, region).tracks.filter { track ->
            track.artists.any { it.equals(artist.name, ignoreCase = true) }
        }
        return ArtistDetailResponse(artist, tracks, albums)
    }

    suspend fun albumDetail(identifier: String): AlbumDetailResponse {
        val (item, region) = catalogItem(identifier, "albums", "tracks,artists")
        val artists = relationship(item, "artists").map { artist(it.jsonObject, region) }
        val album = album(item, region).copy(artists = artists)
        val tracks = relationship(item, "tracks").map { song(it.jsonObject, region) }
        return AlbumDetailResponse(album, tracks)
    }

    suspend fun track(identifier: String): TrackResponse {
        val parts = identifier.split(':')
        if (parts.size != 3 || parts[0] != "apple" || !parts[2].matches(Regex("[0-9]{1,32}"))) {
            throw BadRequestException("Invalid Apple Music track")
        }
        val region = validateStorefront(parts[1])
        val response = get("https://api.music.apple.com/v1/catalog/$region/songs/${parts[2]}") { bearerAuth(tokens.create()) }
        if (response.status == HttpStatusCode.NotFound) throw NotFoundException("Apple Music track not found")
        if (!response.status.isSuccess()) throw HttpException(HttpStatusCode.BadGateway, "apple_music_upstream_error", "Apple Music catalog request failed")
        val data = Json.parseToJsonElement(response.bodyAsText()).jsonObject["data"]?.jsonArray
        return song(data?.firstOrNull()?.jsonObject ?: throw NotFoundException("Apple Music track not found"), region)
    }

    suspend fun charts(storefront: String): TrackPageResponse {
        val region = validateStorefront(storefront)
        val response = get("https://api.music.apple.com/v1/catalog/$region/charts") {
            bearerAuth(tokens.create())
            parameter("types", "songs")
            parameter("limit", 20)
        }
        if (!response.status.isSuccess()) throw HttpException(HttpStatusCode.BadGateway, "apple_music_upstream_error", "Apple Music catalog request failed")
        return chartTracks(Json.parseToJsonElement(response.bodyAsText()).jsonObject, region)
    }

    suspend fun matchIsrc(isrc: String, storefront: String): TrackPageResponse {
        val region = validateStorefront(storefront)
        if (!isrc.matches(Regex("[A-Za-z0-9]{12}"))) throw BadRequestException("Invalid recording code")
        val response = get("https://api.music.apple.com/v1/catalog/$region/songs") {
            bearerAuth(tokens.create())
            parameter("filter[isrc]", isrc.uppercase())
            parameter("limit", 25)
        }
        if (!response.status.isSuccess()) throw HttpException(HttpStatusCode.BadGateway, "apple_music_upstream_error", "Apple Music catalog request failed")
        val data = Json.parseToJsonElement(response.bodyAsText()).jsonObject["data"]?.jsonArray.orEmpty()
        val tracks = data.map { song(it.jsonObject, region) }.filter { it.isrc.equals(isrc, ignoreCase = true) }
        return TrackPageResponse(tracks, tracks.size)
    }

    internal fun chartTracks(root: JsonObject, storefront: String): TrackPageResponse {
        val charts = root["results"]?.jsonObject?.get("songs")?.jsonArray.orEmpty()
        val songs = charts.firstOrNull()?.jsonObject?.get("data")?.jsonArray.orEmpty()
        val tracks = songs.map { song(it.jsonObject, storefront) }
        return TrackPageResponse(tracks, tracks.size)
    }

    internal fun song(item: JsonObject, storefront: String): TrackResponse {
        val id = item.getValue("id").jsonPrimitive.content
        val attributes = item.getValue("attributes").jsonObject
        fun value(key: String) = attributes[key]?.jsonPrimitive?.contentOrNull.orEmpty()
        val image = attributes["artwork"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
            ?.replace("{w}", "300")?.replace("{h}", "300")
        return TrackResponse(
            id = "apple:$storefront:$id", name = value("name"), artists = listOf(value("artistName")),
            album = value("albumName"), imageUrl = image,
            durationMs = attributes["durationInMillis"]?.jsonPrimitive?.longOrNull ?: 0,
            explicit = value("contentRating") == "explicit", uri = id, source = TrackSource.APPLE_MUSIC,
            isrc = value("isrc").ifBlank { null },
        )
    }

    private fun validateStorefront(value: String): String {
        val region = value.lowercase()
        if (!region.matches(Regex("[a-z]{2}"))) throw BadRequestException("Invalid Apple Music storefront")
        return region
    }
}
