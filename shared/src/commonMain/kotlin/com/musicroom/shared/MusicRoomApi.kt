package com.musicroom.shared

import com.musicroom.shared.dto.HealthResponse
import com.musicroom.shared.dto.InfoResponse
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json

data class ClientInfo(
    val platform: String,
    val device: String,
    val appVersion: String,
)

class MusicRoomApi(
    private val baseUrl: String,
    clientInfo: ClientInfo,
    engine: HttpClientEngineFactory<*>,
    extraConfig: HttpClientConfig<*>.() -> Unit = {},
) {
    val http: HttpClient = HttpClient(engine) {
        install(ContentNegotiation) { json(MusicRoomJson) }
        install(WebSockets)
        defaultRequest {
            headers.append(ClientHeaders.PLATFORM, clientInfo.platform)
            headers.append(ClientHeaders.DEVICE, clientInfo.device)
            headers.append(ClientHeaders.APP_VERSION, clientInfo.appVersion)
        }
        extraConfig()
    }

    private fun url(path: String) = baseUrl.trimEnd('/') + path

    suspend fun health(): HealthResponse = http.get(url(ApiRoutes.HEALTH)).body()

    suspend fun info(): InfoResponse = http.get(url(ApiRoutes.INFO)).body()

    fun close() = http.close()
}
