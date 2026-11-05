package com.musicroom.server.modules.premium

import com.musicroom.server.AppModule
import com.musicroom.server.bootstrap
import com.musicroom.server.common.security.PasswordHasher
import com.musicroom.server.config.AppConfig
import com.musicroom.server.config.DatabaseConfig
import com.musicroom.server.config.JwtConfig
import com.musicroom.server.config.SpotifyConfig
import com.musicroom.server.config.StripeConfig
import com.musicroom.server.database.TestDatabase
import com.musicroom.server.modules.premium.stripe.StripeSignature
import com.musicroom.server.modules.spotify.FakeSpotifyApi
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.ApiRoutes
import com.musicroom.shared.MusicRoomJson
import com.musicroom.shared.dto.AddPlaylistTrackRequest
import com.musicroom.shared.dto.AuthResponse
import com.musicroom.shared.dto.BillingUrlResponse
import com.musicroom.shared.dto.CreatePlaylistRequest
import com.musicroom.shared.dto.ErrorResponse
import com.musicroom.shared.dto.HostedTracksResponse
import com.musicroom.shared.dto.PlaylistDetailResponse
import com.musicroom.shared.dto.PlaylistResponse
import com.musicroom.shared.dto.RegisterRequest
import com.musicroom.shared.dto.SubscriptionResponse
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.TrackSource
import com.musicroom.shared.dto.UserResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
class PremiumTest {

    private val stripe = FakeStripeApi()

    private fun config(stripe: StripeConfig) = AppConfig(
        version = "test",
        database = DatabaseConfig("unused", "unused", "unused"),
        jwt = JwtConfig(secret = "x".repeat(32), issuer = "test", audience = "test", realm = "test"),
        spotify = SpotifyConfig(clientId = "id", clientSecret = "secret"),
        stripe = stripe,
    )

    private fun ApplicationTestBuilder.client(stripeConfig: StripeConfig = STRIPE): HttpClient {
        val appModule = AppModule(
            config(stripeConfig),
            TestDatabase.module,
            PasswordHasher(cost = 4),
            emailSender = { _, _, _ -> },
            spotifyApi = FakeSpotifyApi(),
            stripeApi = stripe,
        )
        environment { config = MapApplicationConfig() }
        application { bootstrap(appModule) }
        return createClient { install(ContentNegotiation) { json(MusicRoomJson) } }
    }

    private suspend fun HttpClient.register(): AuthResponse = post(ApiRoutes.AUTH_REGISTER) {
        contentType(ContentType.Application.Json)
        setBody(RegisterRequest("user-${UUID.randomUUID()}@example.com", "hunter42abc", "Zoe ${UUID.randomUUID().toString().take(8)}", acceptedTerms = true))
    }.body()

    /** Signs up, pays on the Stripe page and comes back to the return page. Returns the access token. */
    private suspend fun HttpClient.premiumUser(): String {
        val token = register().accessToken
        val checkout: BillingUrlResponse = post(ApiRoutes.BILLING_CHECKOUT) { bearerAuth(token) }.body()
        val sessionId = checkout.url.substringAfterLast('/')
        stripe.pay(sessionId)
        val page = get(ApiRoutes.BILLING_RETURN) { parameter("session_id", sessionId) }
        assertTrue(page.bodyAsText().contains("Premium"))
        return token
    }

    private suspend fun HttpClient.upload(token: String, audio: ByteArray, title: String = "Demo"): HttpResponse =
        post(ApiRoutes.HOSTED_TRACKS) {
            bearerAuth(token)
            parameter("title", title)
            parameter("artist", "Zoe")
            contentType(ContentType.Audio.MPEG)
            setBody(audio)
        }

    private suspend fun HttpResponse.error(): String = body<ErrorResponse>().error

    @Test
    fun `without Stripe, Premium is off and songs cannot be uploaded`() = testApplication {
        val client = client(StripeConfig())
        val token = client.register().accessToken

        val status: SubscriptionResponse = client.get(ApiRoutes.BILLING) { bearerAuth(token) }.body()
        assertFalse(status.configured)
        val checkout = client.post(ApiRoutes.BILLING_CHECKOUT) { bearerAuth(token) }
        assertEquals(HttpStatusCode.ServiceUnavailable, checkout.status)
        assertEquals(ApiErrors.BILLING_NOT_CONFIGURED, checkout.error())
        val upload = client.upload(token, mp3(frames = 20))
        assertEquals(HttpStatusCode.Forbidden, upload.status)
        assertEquals(ApiErrors.PREMIUM_REQUIRED, upload.error())
    }

    @Test
    fun `paying on the Stripe page gives Premium, and the success page reads it back`() = testApplication {
        val client = client()
        val token = client.register().accessToken
        val before: SubscriptionResponse = client.get(ApiRoutes.BILLING) { bearerAuth(token) }.body()
        assertTrue(before.configured)
        assertFalse(before.premium)
        assertEquals(ApiErrors.PREMIUM_REQUIRED, client.upload(token, mp3(frames = 20)).error())

        val checkout: BillingUrlResponse = client.post(ApiRoutes.BILLING_CHECKOUT) { bearerAuth(token) }.body()
        assertTrue(stripe.lastSuccessUrl!!.endsWith("?session_id={CHECKOUT_SESSION_ID}"))
        val sessionId = checkout.url.substringAfterLast('/')
        stripe.pay(sessionId)
        client.get(ApiRoutes.BILLING_RETURN) { parameter("session_id", sessionId) }

        val after: SubscriptionResponse = client.get(ApiRoutes.BILLING) { bearerAuth(token) }.body()
        assertTrue(after.premium)
        assertEquals(4_102_444_800_000, after.renewsAt)
        val again = client.post(ApiRoutes.BILLING_CHECKOUT) { bearerAuth(token) }
        assertEquals(ApiErrors.ALREADY_PREMIUM, again.error())
        val portal: BillingUrlResponse = client.post(ApiRoutes.BILLING_PORTAL) { bearerAuth(token) }.body()
        assertTrue(portal.url.startsWith("https://billing.stripe.test/"))
    }

    @Test
    fun `the webhook needs Stripe's signature and ends Premium when the subscription is cancelled`() = testApplication {
        val client = client()
        val token = client.premiumUser()
        val subscription = stripe.subscriptions.values.last()
        val payload = """
            {"id":"evt_1","type":"customer.subscription.deleted","data":{"object":
            {"id":"${subscription.id}","customer":"${subscription.customer}","status":"canceled","cancel_at_period_end":false}}}
        """.trimIndent()

        val unsigned = client.post(ApiRoutes.BILLING_WEBHOOK) {
            header("Stripe-Signature", "t=${System.currentTimeMillis() / 1000},v1=bad")
            setBody(payload)
        }
        assertEquals(HttpStatusCode.BadRequest, unsigned.status)
        assertTrue(client.get(ApiRoutes.BILLING) { bearerAuth(token) }.body<SubscriptionResponse>().premium)

        val time = System.currentTimeMillis() / 1000
        val signed = client.post(ApiRoutes.BILLING_WEBHOOK) {
            header("Stripe-Signature", "t=$time,v1=${StripeSignature.sign("$time.$payload", WEBHOOK_SECRET)}")
            setBody(payload)
        }
        assertEquals(HttpStatusCode.OK, signed.status)
        val status: SubscriptionResponse = client.get(ApiRoutes.BILLING) { bearerAuth(token) }.body()
        assertFalse(status.premium)
        assertEquals("canceled", status.status)
    }

    @Test
    fun `a Premium user uploads MP3 songs, shown on their profile, playable and added to playlists`() = testApplication {
        val client = client()
        val token = client.premiumUser()

        val notMp3 = client.upload(token, ByteArray(4_000) { it.toByte() })
        assertEquals(ApiErrors.INVALID_AUDIO, notMp3.error())

        val audio = mp3(frames = 100)
        val uploaded = client.upload(token, audio, title = "Night drive")
        assertEquals(HttpStatusCode.Created, uploaded.status)
        val track: TrackResponse = uploaded.body()
        assertEquals(TrackSource.MUSIC_ROOM, track.source)
        assertTrue(track.id.startsWith(TrackResponse.HOSTED_PREFIX))
        assertEquals("Night drive", track.name)
        // 100 frames of 1152 samples at 44.1 kHz.
        assertEquals(100 * 1152 * 1000L / 44_100, track.durationMs)

        val mine: HostedTracksResponse = client.get(ApiRoutes.HOSTED_TRACKS) { bearerAuth(token) }.body()
        assertEquals(listOf(track.id), mine.items.map { it.id })
        val other = client.register().accessToken
        val me = client.get(ApiRoutes.USERS_ME) { bearerAuth(token) }.body<UserResponse>()
        val onProfile: HostedTracksResponse = client.get(ApiRoutes.userTracks(me.id)) { bearerAuth(other) }.body()
        assertEquals(listOf(track.id), onProfile.items.map { it.id })
        val rawId = track.id.removePrefix(TrackResponse.HOSTED_PREFIX)
        assertContentEquals(audio, client.get(ApiRoutes.hostedTrackAudio(rawId)) { bearerAuth(other) }.readRawBytes())

        val playlist: PlaylistResponse = client.post(ApiRoutes.PLAYLISTS) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreatePlaylistRequest("Mine"))
        }.body()
        val detail: PlaylistDetailResponse = client.post(ApiRoutes.roomPlaylistTracks(playlist.id)) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(AddPlaylistTrackRequest(track.id))
        }.body()
        assertEquals(TrackSource.MUSIC_ROOM, detail.tracks.single().track.source)

        // Only the owner deletes it; it leaves the playlists too.
        assertEquals(HttpStatusCode.NotFound, client.delete(ApiRoutes.hostedTrack(rawId)) { bearerAuth(other) }.status)
        assertEquals(HttpStatusCode.NoContent, client.delete(ApiRoutes.hostedTrack(rawId)) { bearerAuth(token) }.status)
        val emptied: PlaylistDetailResponse = client.get(ApiRoutes.playlist(playlist.id)) { bearerAuth(token) }.body()
        assertTrue(emptied.tracks.isEmpty())
        assertEquals(HttpStatusCode.NotFound, client.get(ApiRoutes.hostedTrackAudio(rawId)) { bearerAuth(token) }.status)
    }

    @Test
    fun `MP3 length is read from the frames, after the ID3 tag`() {
        val tag = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 4, 0, 0, 0, 0, 0, 20) + ByteArray(20)
        assertEquals(50 * 1152 * 1000L / 44_100, Mp3Audio.durationMs(tag + mp3(frames = 50)))
        assertNull(Mp3Audio.durationMs(mp3(frames = 3)))
        assertNull(Mp3Audio.durationMs("hello".encodeToByteArray()))
    }

    companion object {
        private const val WEBHOOK_SECRET = "whsec_test"
        private val STRIPE = StripeConfig(secretKey = "sk_test", webhookSecret = WEBHOOK_SECRET, priceId = "price_1")

        /** [frames] silent MPEG-1 layer III frames, 128 kbit/s at 44.1 kHz (417 bytes each). */
        fun mp3(frames: Int): ByteArray {
            val frame = ByteArray(417).also {
                it[0] = 0xFF.toByte()
                it[1] = 0xFB.toByte()
                it[2] = 0x90.toByte()
                it[3] = 0x00
            }
            return ByteArray(frame.size * frames) { frame[it % frame.size] }
        }
    }
}
