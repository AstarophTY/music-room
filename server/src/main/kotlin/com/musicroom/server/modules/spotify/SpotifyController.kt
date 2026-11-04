package com.musicroom.server.modules.spotify

import com.musicroom.server.common.Controller
import com.musicroom.server.common.exceptions.BadRequestException
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.common.guards.currentUserId
import com.musicroom.server.common.guards.jwtGuard
import com.musicroom.server.common.i18n.requestLanguage
import com.musicroom.server.modules.users.UsersRepository
import com.musicroom.shared.ApiRoutes
import io.ktor.http.ContentType
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post

class SpotifyController(private val service: SpotifyService, private val users: UsersRepository) : Controller {
    override fun Route.routes() {
        // Opened by the browser after the Spotify login: no Music Room token here, the state says who it is.
        get(ApiRoutes.SPOTIFY_CALLBACK) {
            val params = call.request.queryParameters
            val outcome = service.handleCallback(params["code"], params["state"], params["error"])
            call.respondText(SpotifyCallbackPage.html(outcome, call.requestLanguage()), ContentType.Text.Html)
        }
        jwtGuard {
            get(ApiRoutes.SPOTIFY_STATUS) {
                call.respond(service.status(call.currentUserId()))
            }
            post(ApiRoutes.SPOTIFY_CONNECT) {
                call.respond(service.connect(call.currentUserId()))
            }
            delete(ApiRoutes.SPOTIFY) {
                val userId = call.currentUserId()
                service.disconnect(userId)
                call.respond((users.findById(userId) ?: throw NotFoundException("User not found")).toResponse())
            }
            get(ApiRoutes.MUSIC_SEARCH) {
                val query = call.request.queryParameters["q"]?.trim().orEmpty()
                if (query.isEmpty()) throw BadRequestException("Type something to search")
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                call.respond(service.search(call.currentUserId(), query, offset))
            }
            get(ApiRoutes.MUSIC_TRACK) {
                call.respond(service.track(call.currentUserId(), call.parameters["id"].orEmpty()))
            }
            get(ApiRoutes.MUSIC_MY_PLAYLISTS) {
                call.respond(service.myPlaylists(call.currentUserId()))
            }
            get(ApiRoutes.MUSIC_MY_TOP_TRACKS) {
                call.respond(service.topTracks(call.currentUserId()))
            }
            get(ApiRoutes.MUSIC_PLAYLIST_TRACKS) {
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                call.respond(service.playlistTracks(call.currentUserId(), call.parameters["id"].orEmpty(), offset))
            }
        }
    }
}
