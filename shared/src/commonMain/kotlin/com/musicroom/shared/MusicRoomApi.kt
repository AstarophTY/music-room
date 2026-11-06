package com.musicroom.shared

import com.musicroom.shared.dto.AddPlaylistMemberRequest
import com.musicroom.shared.dto.AddPlaylistTrackRequest
import com.musicroom.shared.dto.AlbumDetailResponse
import com.musicroom.shared.dto.ArtistDetailResponse
import com.musicroom.shared.dto.AuthResponse
import com.musicroom.shared.dto.BillingUrlResponse
import com.musicroom.shared.dto.ChangePasswordRequest
import com.musicroom.shared.dto.CreatePlaylistRequest
import com.musicroom.shared.dto.ConversationsResponse
import com.musicroom.shared.dto.CreateEventRequest
import com.musicroom.shared.dto.EventDetailResponse
import com.musicroom.shared.dto.EventLiveEvent
import com.musicroom.shared.dto.EventVoteRequest
import com.musicroom.shared.dto.EventsResponse
import com.musicroom.shared.dto.EventsScope
import com.musicroom.shared.dto.GeoPoint
import com.musicroom.shared.dto.HostedTracksResponse
import com.musicroom.shared.dto.ProposeTrackRequest
import com.musicroom.shared.dto.FriendsResponse
import com.musicroom.shared.dto.MessageResponse
import com.musicroom.shared.dto.MessagesResponse
import com.musicroom.shared.dto.NotificationsResponse
import com.musicroom.shared.dto.PublicUserResponse
import com.musicroom.shared.dto.SendMessageRequest
import com.musicroom.shared.dto.SocialClientEvent
import com.musicroom.shared.dto.SocialEvent
import com.musicroom.shared.dto.SocialCountsResponse
import com.musicroom.shared.dto.SubscriptionResponse
import com.musicroom.shared.dto.UserProfileResponse
import com.musicroom.shared.dto.UsersResponse
import com.musicroom.shared.dto.DevicesResponse
import com.musicroom.shared.dto.ErrorResponse
import com.musicroom.shared.dto.ForgotPasswordRequest
import com.musicroom.shared.dto.HealthResponse
import com.musicroom.shared.dto.InfoResponse
import com.musicroom.shared.dto.LoginRequest
import com.musicroom.shared.dto.OAuthLoginRequest
import com.musicroom.shared.dto.PlayRequest
import com.musicroom.shared.dto.PlaybackStateResponse
import com.musicroom.shared.dto.QueueRequest
import com.musicroom.shared.dto.QueueResponse
import com.musicroom.shared.dto.PlaylistAccess
import com.musicroom.shared.dto.PlaylistDetailResponse
import com.musicroom.shared.dto.PlaylistListResponse
import com.musicroom.shared.dto.PlaylistResponse
import com.musicroom.shared.dto.PlaylistsResponse
import com.musicroom.shared.dto.RecommendationsResponse
import com.musicroom.shared.dto.RecoveryCodesResponse
import com.musicroom.shared.dto.RefreshTokenRequest
import com.musicroom.shared.dto.RegisterRequest
import com.musicroom.shared.dto.SearchResponse
import com.musicroom.shared.dto.ResetPasswordRequest
import com.musicroom.shared.dto.SpotifyConnectResponse
import com.musicroom.shared.dto.AppleMusicDeveloperToken
import com.musicroom.shared.dto.SpotifyStatusResponse
import com.musicroom.shared.dto.TrackPageResponse
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TransferRequest
import com.musicroom.shared.dto.TwoFactorChallengeResponse
import com.musicroom.shared.dto.TwoFactorCodeRequest
import com.musicroom.shared.dto.TwoFactorLoginRequest
import com.musicroom.shared.dto.TwoFactorResendRequest
import com.musicroom.shared.dto.TwoFactorSetupResponse
import com.musicroom.shared.dto.UpdatePlaylistMemberRequest
import com.musicroom.shared.dto.UpdatePlaylistRequest
import com.musicroom.shared.dto.UpdateProfileRequest
import com.musicroom.shared.dto.UpdateSocialLinksRequest
import com.musicroom.shared.dto.UserResponse
import com.musicroom.shared.dto.VerifyEmailRequest
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * HTTP client for the Music Room backend.
 *
 * Keeps the short-lived access token in memory and the refresh token in [tokenStore].
 * When a call returns 401, it refreshes the tokens once and retries.
 */
class MusicRoomApi(
    val baseUrl: String,
    clientInfo: ClientInfo,
    engine: HttpClientEngineFactory<*>,
    private val tokenStore: TokenStore,
    extraConfig: HttpClientConfig<*>.() -> Unit = {},
) {
    val http: HttpClient = HttpClient(engine) {
        install(ContentNegotiation) { json(MusicRoomJson) }
        install(WebSockets)
        defaultRequest {
            headers.append(ClientHeaders.PLATFORM, clientInfo.platform)
            headers.append(ClientHeaders.DEVICE, clientInfo.device)
            headers.append(ClientHeaders.APP_VERSION, clientInfo.appVersion)
            // Runs for every request, so a language change is picked up right away.
            headers.append(HttpHeaders.AcceptLanguage, language)
        }
        extraConfig()
    }

    /** Device language (e.g. "fr"), sent as Accept-Language so server emails and messages match it. */
    var language: String = "en"

    /** Without the Music Room headers: for public files such as album covers. */
    private val plainHttp: HttpClient = HttpClient(engine)

    private var accessToken: String? = null
    private val refreshLock = Mutex()

    val hasSession: Boolean get() = tokenStore.refreshToken != null

    private fun url(path: String) = baseUrl.trimEnd('/') + path

    suspend fun health(): HealthResponse = http.get(url(ApiRoutes.HEALTH)).body()

    suspend fun info(): InfoResponse = http.get(url(ApiRoutes.INFO)).body()

    suspend fun register(request: RegisterRequest): UserResponse =
        startSession(postJson(ApiRoutes.AUTH_REGISTER, request).bodyOrThrow())

    /** Throws [TwoFactorRequiredException] if the account has 2FA. */
    suspend fun login(request: LoginRequest): UserResponse = startSessionOrChallenge(postJson(ApiRoutes.AUTH_LOGIN, request))

    /**
     * Signs in with a Google/Apple ID token. Throws ApiException(error = ApiErrors.TERMS_REQUIRED) for a new account,
     * and [TwoFactorRequiredException] if the account has 2FA.
     */
    suspend fun oauthLogin(request: OAuthLoginRequest): UserResponse =
        startSessionOrChallenge(postJson(ApiRoutes.AUTH_OAUTH, request))

    /** Second step of a login with 2FA: the code from the authenticator app, or a recovery code. */
    suspend fun verifyTwoFactor(request: TwoFactorLoginRequest): UserResponse =
        startSession(postJson(ApiRoutes.AUTH_2FA_VERIFY, request).bodyOrThrow())

    /** Starts 2FA setup: returns the secret to add to the authenticator app. */
    suspend fun setupTwoFactor(): TwoFactorSetupResponse =
        authorized(HttpMethod.Post, ApiRoutes.AUTH_2FA_SETUP).bodyOrThrow()

    /** Confirms setup with a first code. Returns the recovery codes (shown once). */
    suspend fun enableTwoFactor(request: TwoFactorCodeRequest): RecoveryCodesResponse =
        authorized(HttpMethod.Post, ApiRoutes.AUTH_2FA_ENABLE) { jsonBody(request) }.bodyOrThrow()

    /** Turns 2FA off (either method). Needs a current code: from the app, a recovery code, or an emailed one. */
    suspend fun disableTwoFactor(request: TwoFactorCodeRequest): UserResponse =
        authorized(HttpMethod.Post, ApiRoutes.AUTH_2FA_DISABLE) { jsonBody(request) }.bodyOrThrow()

    /** Emails a code to turn email 2FA on, or to turn it off. */
    suspend fun sendTwoFactorEmailCode() {
        authorized(HttpMethod.Post, ApiRoutes.AUTH_2FA_EMAIL_CODE).throwIfError()
    }

    /** Turns email 2FA on with the code from [sendTwoFactorEmailCode]. */
    suspend fun enableEmailTwoFactor(request: TwoFactorCodeRequest): UserResponse =
        authorized(HttpMethod.Post, ApiRoutes.AUTH_2FA_EMAIL_ENABLE) { jsonBody(request) }.bodyOrThrow()

    /** During a login with email 2FA: emails a new code. */
    suspend fun resendTwoFactorEmail(request: TwoFactorResendRequest) {
        postJson(ApiRoutes.AUTH_2FA_EMAIL_RESEND, request).throwIfError()
    }

    /** Uploads a profile photo (JPEG or PNG). The server crops and resizes it. */
    suspend fun uploadAvatar(image: ByteArray, contentType: ContentType): UserResponse =
        authorized(HttpMethod.Put, ApiRoutes.USERS_ME_AVATAR) {
            contentType(contentType)
            setBody(image)
        }.bodyOrThrow()

    /** Replaces the social links shown on the profile. */
    suspend fun updateSocialLinks(request: UpdateSocialLinksRequest): UserResponse =
        authorized(HttpMethod.Put, ApiRoutes.USERS_ME_SOCIAL_LINKS) { jsonBody(request) }.bodyOrThrow()

    suspend fun spotifyStatus(): SpotifyStatusResponse = authorized(HttpMethod.Get, ApiRoutes.SPOTIFY_STATUS).bodyOrThrow()

    suspend fun appleMusicDeveloperToken(): AppleMusicDeveloperToken =
        authorized(HttpMethod.Get, ApiRoutes.APPLE_MUSIC_DEVELOPER_TOKEN).bodyOrThrow()

    suspend fun searchAppleMusic(query: String, storefront: String): TrackPageResponse =
        authorized(HttpMethod.Get, ApiRoutes.APPLE_MUSIC_SEARCH) {
            url.parameters.append("q", query)
            url.parameters.append("storefront", storefront)
        }.bodyOrThrow()

    /** Returns the Spotify page to open in the browser to connect the account. */
    suspend fun spotifyConnect(): SpotifyConnectResponse =
        authorized(HttpMethod.Post, ApiRoutes.SPOTIFY_CONNECT).bodyOrThrow()

    suspend fun spotifyDisconnect(): UserResponse = authorized(HttpMethod.Delete, ApiRoutes.SPOTIFY).bodyOrThrow()

    suspend fun searchTracks(query: String, offset: Int = 0): TrackPageResponse =
        authorized(HttpMethod.Get, ApiRoutes.MUSIC_SEARCH) {
            url.parameters.append("q", query)
            url.parameters.append("offset", offset.toString())
        }.bodyOrThrow()

    /** Tracks, artists and albums matching [query]. */
    suspend fun search(query: String): SearchResponse =
        authorized(HttpMethod.Get, ApiRoutes.MUSIC_SEARCH_ALL) { url.parameters.append("q", query) }.bodyOrThrow()

    suspend fun artist(id: String): ArtistDetailResponse = authorized(HttpMethod.Get, ApiRoutes.artist(id)).bodyOrThrow()

    suspend fun album(id: String): AlbumDetailResponse = authorized(HttpMethod.Get, ApiRoutes.album(id)).bodyOrThrow()

    suspend fun track(id: String): TrackResponse = authorized(HttpMethod.Get, ApiRoutes.track(id)).bodyOrThrow()

    suspend fun myPlaylists(): PlaylistsResponse = authorized(HttpMethod.Get, ApiRoutes.MUSIC_MY_PLAYLISTS).bodyOrThrow()

    suspend fun playlistTracks(id: String, offset: Int = 0): TrackPageResponse =
        authorized(HttpMethod.Get, ApiRoutes.playlistTracks(id)) {
            url.parameters.append("offset", offset.toString())
        }.bodyOrThrow()

    suspend fun myTopTracks(): TrackPageResponse = authorized(HttpMethod.Get, ApiRoutes.MUSIC_MY_TOP_TRACKS).bodyOrThrow()

    suspend fun recommendations(): RecommendationsResponse =
        authorized(HttpMethod.Get, ApiRoutes.MUSIC_RECOMMENDATIONS).bodyOrThrow()

    /** What Spotify is playing on the user's devices. */
    suspend fun playbackState(): PlaybackStateResponse = authorized(HttpMethod.Get, ApiRoutes.MUSIC_PLAYER).bodyOrThrow()

    /** Plays [uris], or the playlist [contextUri], from [index] on the user's Spotify device; neither resumes. */
    suspend fun play(uris: List<String> = emptyList(), index: Int = 0, contextUri: String? = null) {
        authorized(HttpMethod.Put, ApiRoutes.MUSIC_PLAYER_PLAY) { jsonBody(PlayRequest(uris, index, contextUri)) }.throwIfError()
    }

    suspend fun pause() {
        authorized(HttpMethod.Put, ApiRoutes.MUSIC_PLAYER_PAUSE).throwIfError()
    }

    suspend fun skipNext() {
        authorized(HttpMethod.Post, ApiRoutes.MUSIC_PLAYER_NEXT).throwIfError()
    }

    suspend fun skipPrevious() {
        authorized(HttpMethod.Post, ApiRoutes.MUSIC_PLAYER_PREVIOUS).throwIfError()
    }

    suspend fun seek(positionMs: Long) {
        authorized(HttpMethod.Put, ApiRoutes.MUSIC_PLAYER_SEEK) {
            url.parameters.append("positionMs", positionMs.toString())
        }.throwIfError()
    }

    /** The user's devices with Spotify open. */
    suspend fun devices(): DevicesResponse = authorized(HttpMethod.Get, ApiRoutes.MUSIC_PLAYER_DEVICES).bodyOrThrow()

    /** Moves playback to another device; [play] starts it there. */
    suspend fun transferPlayback(deviceId: String, play: Boolean) {
        authorized(HttpMethod.Put, ApiRoutes.MUSIC_PLAYER_DEVICE) { jsonBody(TransferRequest(deviceId, play)) }.throwIfError()
    }

    /** The next tracks Spotify will play. */
    suspend fun queue(): QueueResponse = authorized(HttpMethod.Get, ApiRoutes.MUSIC_PLAYER_QUEUE).bodyOrThrow()

    /** Adds track [uris] at the end of the Spotify queue, in order. */
    suspend fun addToQueue(uris: List<String>) {
        authorized(HttpMethod.Post, ApiRoutes.MUSIC_PLAYER_QUEUE) { jsonBody(QueueRequest(uris)) }.throwIfError()
    }

    /** Music Room playlists the user owns or was added to. */
    suspend fun roomPlaylists(): PlaylistListResponse = authorized(HttpMethod.Get, ApiRoutes.PLAYLISTS).bodyOrThrow()

    suspend fun createPlaylist(request: CreatePlaylistRequest): PlaylistResponse =
        authorized(HttpMethod.Post, ApiRoutes.PLAYLISTS) { jsonBody(request) }.bodyOrThrow()

    suspend fun roomPlaylist(id: String): PlaylistDetailResponse = authorized(HttpMethod.Get, ApiRoutes.playlist(id)).bodyOrThrow()

    suspend fun updatePlaylist(id: String, request: UpdatePlaylistRequest): PlaylistDetailResponse =
        authorized(HttpMethod.Patch, ApiRoutes.playlist(id)) { jsonBody(request) }.bodyOrThrow()

    suspend fun deletePlaylist(id: String) {
        authorized(HttpMethod.Delete, ApiRoutes.playlist(id)).throwIfError()
    }

    /** Uploads the playlist photo (JPEG or PNG). The server crops and resizes it. */
    suspend fun uploadPlaylistCover(id: String, image: ByteArray, contentType: ContentType): PlaylistDetailResponse =
        authorized(HttpMethod.Put, ApiRoutes.playlistCover(id)) {
            contentType(contentType)
            setBody(image)
        }.bodyOrThrow()

    suspend fun deletePlaylistCover(id: String): PlaylistDetailResponse =
        authorized(HttpMethod.Delete, ApiRoutes.playlistCover(id)).bodyOrThrow()

    /** Playlist photo bytes (JPEG). */
    suspend fun playlistCover(id: String): ByteArray = authorized(HttpMethod.Get, ApiRoutes.playlistCover(id)).bodyOrThrow()

    /** Adds a Spotify track at the end of the playlist. */
    suspend fun addPlaylistTrack(id: String, trackId: String): PlaylistDetailResponse =
        authorized(HttpMethod.Post, ApiRoutes.roomPlaylistTracks(id)) { jsonBody(AddPlaylistTrackRequest(trackId)) }.bodyOrThrow()

    suspend fun removePlaylistTrack(id: String, entryId: String): PlaylistDetailResponse =
        authorized(HttpMethod.Delete, ApiRoutes.roomPlaylistTrack(id, entryId)).bodyOrThrow()

    suspend fun addPlaylistMember(id: String, email: String, access: PlaylistAccess): PlaylistDetailResponse =
        authorized(HttpMethod.Post, ApiRoutes.playlistMembers(id)) { jsonBody(AddPlaylistMemberRequest(email, access)) }.bodyOrThrow()

    suspend fun updatePlaylistMember(id: String, userId: String, access: PlaylistAccess): PlaylistDetailResponse =
        authorized(HttpMethod.Patch, ApiRoutes.playlistMember(id, userId)) { jsonBody(UpdatePlaylistMemberRequest(access)) }.bodyOrThrow()

    /** Removes a member (owner), or leaves the playlist when [userId] is the user's own id. */
    suspend fun removePlaylistMember(id: String, userId: String) {
        authorized(HttpMethod.Delete, ApiRoutes.playlistMember(id, userId)).throwIfError()
    }

    /** Adds one of the user's friends to the playlist (they get a notification). */
    suspend fun addPlaylistFriend(id: String, userId: String, access: PlaylistAccess): PlaylistDetailResponse =
        authorized(HttpMethod.Post, ApiRoutes.playlistMembers(id)) {
            jsonBody(AddPlaylistMemberRequest(access = access, userId = userId))
        }.bodyOrThrow()

    /** Users whose name contains [query], with how they relate to the user. */
    suspend fun searchUsers(query: String): UsersResponse =
        authorized(HttpMethod.Get, ApiRoutes.USERS_SEARCH) { url.parameters.append("q", query) }.bodyOrThrow()

    suspend fun userProfile(userId: String): UserProfileResponse =
        authorized(HttpMethod.Get, ApiRoutes.userProfile(userId)).bodyOrThrow()

    suspend fun friends(): FriendsResponse = authorized(HttpMethod.Get, ApiRoutes.FRIENDS).bodyOrThrow()

    /** Asks [userId] to be friends, or accepts their request. Returns them with the new status. */
    suspend fun addFriend(userId: String): PublicUserResponse =
        authorized(HttpMethod.Post, ApiRoutes.friend(userId)).bodyOrThrow()

    /** Removes a friend, takes back a request, or declines one. */
    suspend fun removeFriend(userId: String) {
        authorized(HttpMethod.Delete, ApiRoutes.friend(userId)).throwIfError()
    }

    suspend fun conversations(): ConversationsResponse = authorized(HttpMethod.Get, ApiRoutes.CONVERSATIONS).bodyOrThrow()

    /** A page of the chat with [userId], oldest first; [before] (epoch ms) loads older messages. */
    suspend fun messages(userId: String, before: Long? = null): MessagesResponse =
        authorized(HttpMethod.Get, ApiRoutes.conversationMessages(userId)) {
            before?.let { url.parameters.append("before", it.toString()) }
        }.bodyOrThrow()

    suspend fun sendMessage(userId: String, body: String): MessageResponse =
        authorized(HttpMethod.Post, ApiRoutes.conversationMessages(userId)) { jsonBody(SendMessageRequest(body)) }.bodyOrThrow()

    /** The user saw the chat with [userId]. */
    suspend fun markConversationRead(userId: String) {
        authorized(HttpMethod.Post, ApiRoutes.conversationRead(userId)).throwIfError()
    }

    suspend fun notifications(): NotificationsResponse = authorized(HttpMethod.Get, ApiRoutes.NOTIFICATIONS).bodyOrThrow()

    suspend fun markNotificationsRead() {
        authorized(HttpMethod.Post, ApiRoutes.NOTIFICATIONS_READ).throwIfError()
    }

    suspend fun socialCounts(): SocialCountsResponse = authorized(HttpMethod.Get, ApiRoutes.SOCIAL_COUNTS).bodyOrThrow()

    /**
     * Listens to the live social events until the connection closes (or the caller is cancelled): [onOpen] runs
     * once connected, [onEvent] for each event, and what arrives in [outgoing] is sent to the server.
     * A first API call makes sure the access token is fresh, since a WebSocket cannot refresh it.
     */
    suspend fun socialEvents(
        outgoing: ReceiveChannel<SocialClientEvent>,
        onOpen: suspend () -> Unit,
        onEvent: suspend (SocialEvent) -> Unit,
    ) {
        socialCounts()
        val token = accessToken ?: throw notLoggedIn()
        val wsUrl = url(ApiRoutes.SOCIAL_WS).replaceFirst("http", "ws")
        http.webSocket({
            url.takeFrom(wsUrl)
            url.parameters.append("token", token)
        }) {
            val sender = launch {
                for (event in outgoing) send(Frame.Text(MusicRoomJson.encodeToString(SocialClientEvent.serializer(), event)))
            }
            try {
                onOpen()
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    // An event this version does not know is skipped.
                    val event = runCatching { MusicRoomJson.decodeFromString(SocialEvent.serializer(), frame.readText()) }.getOrNull()
                    if (event != null) onEvent(event)
                }
            } finally {
                sender.cancel()
            }
        }
    }

    /** Events of [scope]; [near] (the user's position) is needed for [EventsScope.NEARBY]. */
    suspend fun events(scope: EventsScope, near: GeoPoint? = null): EventsResponse =
        authorized(HttpMethod.Get, ApiRoutes.EVENTS) {
            url.parameters.append("scope", scope.name.lowercase())
            near?.let {
                url.parameters.append("lat", it.latitude.toString())
                url.parameters.append("lng", it.longitude.toString())
            }
        }.bodyOrThrow()

    suspend fun createEvent(request: CreateEventRequest): EventDetailResponse =
        authorized(HttpMethod.Post, ApiRoutes.EVENTS) { jsonBody(request) }.bodyOrThrow()

    suspend fun event(id: String): EventDetailResponse = authorized(HttpMethod.Get, ApiRoutes.event(id)).bodyOrThrow()

    suspend fun updateEvent(id: String, request: CreateEventRequest): EventDetailResponse =
        authorized(HttpMethod.Put, ApiRoutes.event(id)) { jsonBody(request) }.bodyOrThrow()

    suspend fun deleteEvent(id: String) {
        authorized(HttpMethod.Delete, ApiRoutes.event(id)).throwIfError()
    }

    /** Proposes a Spotify track (with the user's vote). [location]: needed when votes are limited to an area. */
    suspend fun proposeEventTrack(id: String, trackId: String, location: GeoPoint? = null): EventDetailResponse =
        authorized(HttpMethod.Post, ApiRoutes.eventTracks(id)) { jsonBody(ProposeTrackRequest(trackId, location)) }.bodyOrThrow()

    suspend fun removeEventTrack(id: String, trackId: String): EventDetailResponse =
        authorized(HttpMethod.Delete, ApiRoutes.eventTrack(id, trackId)).bodyOrThrow()

    suspend fun voteEventTrack(id: String, trackId: String, location: GeoPoint? = null): EventDetailResponse =
        authorized(HttpMethod.Post, ApiRoutes.eventTrackVote(id, trackId)) { jsonBody(EventVoteRequest(location)) }.bodyOrThrow()

    suspend fun unvoteEventTrack(id: String, trackId: String): EventDetailResponse =
        authorized(HttpMethod.Delete, ApiRoutes.eventTrackVote(id, trackId)).bodyOrThrow()

    suspend fun inviteToEvent(id: String, userId: String): EventDetailResponse =
        authorized(HttpMethod.Post, ApiRoutes.eventInvited(id, userId)).bodyOrThrow()

    /** The owner takes an invitation back, or the user leaves ([userId] = their own id). */
    suspend fun uninviteFromEvent(id: String, userId: String) {
        authorized(HttpMethod.Delete, ApiRoutes.eventInvited(id, userId)).throwIfError()
    }

    suspend fun playEvent(id: String): EventDetailResponse = authorized(HttpMethod.Post, ApiRoutes.eventPlay(id)).bodyOrThrow()

    suspend fun pauseEvent(id: String): EventDetailResponse = authorized(HttpMethod.Post, ApiRoutes.eventPause(id)).bodyOrThrow()

    suspend fun skipEvent(id: String): EventDetailResponse = authorized(HttpMethod.Post, ApiRoutes.eventSkip(id)).bodyOrThrow()

    suspend fun stopEvent(id: String): EventDetailResponse = authorized(HttpMethod.Post, ApiRoutes.eventStop(id)).bodyOrThrow()

    /**
     * Follows the event [id] until the connection closes (or the caller is cancelled): [onEvent] gets its state on
     * joining and after each change. A first API call makes sure the access token is fresh.
     */
    suspend fun eventLive(id: String, onEvent: suspend (EventLiveEvent) -> Unit) {
        event(id)
        val token = accessToken ?: throw notLoggedIn()
        val wsUrl = url(ApiRoutes.eventWs(id)).replaceFirst("http", "ws")
        http.webSocket({
            url.takeFrom(wsUrl)
            url.parameters.append("token", token)
        }) {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val event = runCatching { MusicRoomJson.decodeFromString(EventLiveEvent.serializer(), frame.readText()) }.getOrNull()
                if (event != null) onEvent(event)
            }
        }
    }

    /** Downloads a public file (album cover...). */
    suspend fun download(url: String): ByteArray = plainHttp.get(url).bodyOrThrow()

    suspend fun deleteAvatar(): UserResponse = authorized(HttpMethod.Delete, ApiRoutes.USERS_ME_AVATAR).bodyOrThrow()

    /** Profile photo bytes (JPEG). */
    suspend fun avatar(userId: String): ByteArray = authorized(HttpMethod.Get, ApiRoutes.userAvatar(userId)).bodyOrThrow()

    /** The user's Music Room Premium subscription. */
    suspend fun subscription(): SubscriptionResponse = authorized(HttpMethod.Get, ApiRoutes.BILLING).bodyOrThrow()

    /** The Stripe payment page to subscribe to Premium. */
    suspend fun premiumCheckout(): BillingUrlResponse = authorized(HttpMethod.Post, ApiRoutes.BILLING_CHECKOUT).bodyOrThrow()

    /** The Stripe page to manage (or cancel) the subscription. */
    suspend fun billingPortal(): BillingUrlResponse = authorized(HttpMethod.Post, ApiRoutes.BILLING_PORTAL).bodyOrThrow()

    /** Songs the user uploaded to Music Room. */
    suspend fun myHostedTracks(): HostedTracksResponse = authorized(HttpMethod.Get, ApiRoutes.HOSTED_TRACKS).bodyOrThrow()

    /** Songs another user uploaded, shown on their profile. */
    suspend fun userTracks(userId: String): HostedTracksResponse = authorized(HttpMethod.Get, ApiRoutes.userTracks(userId)).bodyOrThrow()

    /** Uploads an MP3 file as a song of the user (Premium). */
    suspend fun uploadHostedTrack(audio: ByteArray, title: String, artist: String): TrackResponse =
        authorized(HttpMethod.Post, ApiRoutes.HOSTED_TRACKS) {
            parameter("title", title)
            parameter("artist", artist)
            contentType(ContentType.Audio.MPEG)
            setBody(audio)
        }.bodyOrThrow()

    suspend fun deleteHostedTrack(id: String) {
        authorized(HttpMethod.Delete, ApiRoutes.hostedTrack(id)).throwIfError()
    }

    /** The MP3 file of an uploaded song, played by the app. */
    suspend fun hostedTrackAudio(id: String): ByteArray = authorized(HttpMethod.Get, ApiRoutes.hostedTrackAudio(id)).bodyOrThrow()

    /** Checks the code sent by email and returns the updated user (emailVerified = true). */
    suspend fun verifyEmail(request: VerifyEmailRequest): UserResponse =
        authorized(HttpMethod.Post, ApiRoutes.AUTH_EMAIL_VERIFY) { jsonBody(request) }.bodyOrThrow()

    /** Sends a new verification code (the previous one stops working). */
    suspend fun resendVerificationEmail() {
        authorized(HttpMethod.Post, ApiRoutes.AUTH_EMAIL_RESEND).throwIfError()
    }

    /** Emails a password reset code. The server answers the same way whether the account exists or not. */
    suspend fun forgotPassword(request: ForgotPasswordRequest) {
        postJson(ApiRoutes.AUTH_PASSWORD_FORGOT, request).throwIfError()
    }

    /** Sets a new password with the emailed code, signs out every other device and logs in here. */
    suspend fun resetPassword(request: ResetPasswordRequest): UserResponse =
        startSessionOrChallenge(postJson(ApiRoutes.AUTH_PASSWORD_RESET, request))

    /** Reopens the saved session, or returns null if there is none or it is no longer valid. */
    suspend fun restoreSession(): UserResponse? {
        if (!hasSession || !refresh(failedAccessToken = null)) return null
        return me()
    }

    suspend fun logout() {
        val refreshToken = tokenStore.refreshToken
        clearSession()
        if (refreshToken != null) {
            runCatching { postJson(ApiRoutes.AUTH_LOGOUT, RefreshTokenRequest(refreshToken)) }
        }
    }

    suspend fun me(): UserResponse = authorized(HttpMethod.Get, ApiRoutes.USERS_ME).bodyOrThrow()

    suspend fun updateProfile(request: UpdateProfileRequest): UserResponse =
        authorized(HttpMethod.Patch, ApiRoutes.USERS_ME) { jsonBody(request) }.bodyOrThrow()

    /** Changes the password. The server signs out every other session and returns new tokens. */
    suspend fun changePassword(request: ChangePasswordRequest): UserResponse =
        startSession(authorized(HttpMethod.Post, ApiRoutes.AUTH_PASSWORD) { jsonBody(request) }.bodyOrThrow())

    fun close() {
        http.close()
        plainHttp.close()
    }

    private fun startSession(response: AuthResponse): UserResponse {
        accessToken = response.accessToken
        tokenStore.refreshToken = response.refreshToken
        return response.user
    }

    /** 202 = the account has 2FA: no tokens yet, the user must type a code. */
    private suspend fun startSessionOrChallenge(response: HttpResponse): UserResponse {
        if (response.status == HttpStatusCode.Accepted) {
            val challenge = response.body<TwoFactorChallengeResponse>()
            throw TwoFactorRequiredException(challenge.challengeToken, challenge.method)
        }
        return startSession(response.bodyOrThrow())
    }

    private fun clearSession() {
        accessToken = null
        tokenStore.refreshToken = null
    }

    private suspend inline fun <reified B : Any> postJson(path: String, body: B): HttpResponse =
        http.post(url(path)) { jsonBody(body) }

    /** Gets new tokens. Returns false (and forgets the session) if the server rejects the refresh token. */
    private suspend fun refresh(failedAccessToken: String?): Boolean = refreshLock.withLock {
        // Another call already refreshed while we were waiting for the lock.
        if (accessToken != null && accessToken != failedAccessToken) return true
        val refreshToken = tokenStore.refreshToken ?: return false
        val response = postJson(ApiRoutes.AUTH_REFRESH, RefreshTokenRequest(refreshToken))
        if (response.status == HttpStatusCode.Unauthorized) {
            clearSession()
            return false
        }
        startSession(response.bodyOrThrow())
        true
    }

    private suspend fun authorized(
        method: HttpMethod,
        path: String,
        configure: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        val fullUrl = url(path)
        val send: suspend (String) -> HttpResponse = { token ->
            http.request(fullUrl) {
                this.method = method
                bearerAuth(token)
                configure()
            }
        }
        val token = accessToken ?: run {
            if (!refresh(failedAccessToken = null)) throw notLoggedIn()
            accessToken ?: throw notLoggedIn()
        }
        val response = send(token)
        if (response.status != HttpStatusCode.Unauthorized) return response
        if (!refresh(failedAccessToken = token)) throw notLoggedIn()
        return send(accessToken ?: throw notLoggedIn())
    }

    private inline fun <reified B : Any> HttpRequestBuilder.jsonBody(body: B) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private fun notLoggedIn() =
        ApiException(401, ApiErrors.SESSION_EXPIRED, "Your session has expired, please log in again")

    private suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
        throwIfError()
        return body()
    }

    private suspend fun HttpResponse.throwIfError() {
        if (status.isSuccess()) return
        val error = runCatching { body<ErrorResponse>() }.getOrNull()
        throw ApiException(status.value, error?.error ?: "http_${status.value}", error?.message)
    }
}
