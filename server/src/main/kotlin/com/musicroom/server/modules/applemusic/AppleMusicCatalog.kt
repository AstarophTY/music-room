package com.musicroom.server.modules.applemusic

import com.musicroom.server.common.exceptions.BadRequestException
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.shared.dto.TrackPageResponse
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TrackSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
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

    suspend fun search(query: String, storefront: String): TrackPageResponse {
        val region = validateStorefront(storefront)
        if (query.isBlank() || query.length > 200) throw BadRequestException("Invalid search query")
        val response = client.get("https://api.music.apple.com/v1/catalog/$region/search") {
            bearerAuth(tokens.create())
            parameter("term", query)
            parameter("types", "songs")
            parameter("limit", 25)
        }
        check(response.status.isSuccess()) { "Apple Music catalog search failed (${response.status.value})" }
        val root = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val songs = root["results"]?.jsonObject?.get("songs")?.jsonObject?.get("data")?.jsonArray.orEmpty()
        val tracks = songs.map { song(it.jsonObject, region) }
        return TrackPageResponse(tracks, tracks.size)
    }

    suspend fun track(identifier: String): TrackResponse {
        val parts = identifier.split(':')
        if (parts.size != 3 || parts[0] != "apple" || !parts[2].matches(Regex("[0-9]{1,32}"))) {
            throw BadRequestException("Invalid Apple Music track")
        }
        val region = validateStorefront(parts[1])
        val response = client.get("https://api.music.apple.com/v1/catalog/$region/songs/${parts[2]}") { bearerAuth(tokens.create()) }
        if (response.status == HttpStatusCode.NotFound) throw NotFoundException("Apple Music track not found")
        check(response.status.isSuccess()) { "Apple Music catalog request failed (${response.status.value})" }
        val data = Json.parseToJsonElement(response.bodyAsText()).jsonObject["data"]?.jsonArray
        return song(data?.firstOrNull()?.jsonObject ?: throw NotFoundException("Apple Music track not found"), region)
    }

    suspend fun charts(storefront: String): TrackPageResponse {
        val region = validateStorefront(storefront)
        val response = client.get("https://api.music.apple.com/v1/catalog/$region/charts") {
            bearerAuth(tokens.create())
            parameter("types", "songs")
            parameter("limit", 20)
        }
        check(response.status.isSuccess()) { "Apple Music charts request failed (${response.status.value})" }
        return chartTracks(Json.parseToJsonElement(response.bodyAsText()).jsonObject, region)
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
        )
    }

    private fun validateStorefront(value: String): String {
        val region = value.lowercase()
        if (!region.matches(Regex("[a-z]{2}"))) throw BadRequestException("Invalid Apple Music storefront")
        return region
    }
}
