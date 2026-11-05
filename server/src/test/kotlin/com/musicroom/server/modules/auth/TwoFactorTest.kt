package com.musicroom.server.modules.auth

import com.musicroom.server.AppModule
import com.musicroom.server.bootstrap
import com.musicroom.server.common.security.PasswordHasher
import com.musicroom.server.common.security.Totp
import com.musicroom.server.config.AppConfig
import com.musicroom.server.config.DatabaseConfig
import com.musicroom.server.config.JwtConfig
import com.musicroom.server.database.TestDatabase
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.ApiRoutes
import com.musicroom.shared.MusicRoomJson
import com.musicroom.shared.dto.AuthResponse
import com.musicroom.shared.dto.ErrorResponse
import com.musicroom.shared.dto.LoginRequest
import com.musicroom.shared.dto.RecoveryCodesResponse
import com.musicroom.shared.dto.RegisterRequest
import com.musicroom.shared.dto.TwoFactorChallengeResponse
import com.musicroom.shared.dto.TwoFactorCodeRequest
import com.musicroom.shared.dto.TwoFactorLoginRequest
import com.musicroom.shared.dto.TwoFactorSetupResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
class TwoFactorTest {

    private val config = AppConfig(
        version = "test",
        database = DatabaseConfig("unused", "unused", "unused"),
        jwt = JwtConfig(secret = "x".repeat(32), issuer = "test", audience = "test", realm = "test"),
    )
    private val appModule = AppModule(config, TestDatabase.module, PasswordHasher(cost = 4), emailSender = { _, _, _ -> })

    private fun ApplicationTestBuilder.client(): HttpClient {
        environment { config = MapApplicationConfig() }
        application { bootstrap(appModule) }
        return createClient { install(ContentNegotiation) { json(MusicRoomJson) } }
    }

    private suspend inline fun <reified T : Any> HttpClient.postJson(path: String, body: T, token: String? = null): HttpResponse =
        post(path) {
            contentType(ContentType.Application.Json)
            setBody(body)
            token?.let { bearerAuth(it) }
        }

    private fun codeAt(secret: String, offsetSteps: Long = 0) =
        Totp.code(secret, Totp.step(Instant.now().epochSecond) + offsetSteps)

    /** Signs up and turns 2FA on. Returns the email, the TOTP secret and the recovery codes. */
    private suspend fun HttpClient.userWithTwoFactor(): Triple<String, String, List<String>> {
        val email = "user-${UUID.randomUUID()}@example.com"
        val auth = postJson(ApiRoutes.AUTH_REGISTER, RegisterRequest(email, PASSWORD, "Alice ${UUID.randomUUID().toString().take(8)}", acceptedTerms = true)).body<AuthResponse>()
        val setup = post(ApiRoutes.AUTH_2FA_SETUP) { bearerAuth(auth.accessToken) }.body<TwoFactorSetupResponse>()
        assertTrue(setup.otpauthUri.startsWith("otpauth://totp/"))
        val enabled = postJson(ApiRoutes.AUTH_2FA_ENABLE, TwoFactorCodeRequest(codeAt(setup.secret)), auth.accessToken)
        assertEquals(HttpStatusCode.OK, enabled.status)
        val result = enabled.body<RecoveryCodesResponse>()
        assertTrue(result.user.twoFactorEnabled)
        return Triple(email, setup.secret, result.recoveryCodes)
    }

    private suspend fun HttpClient.challenge(email: String): String {
        val login = postJson(ApiRoutes.AUTH_LOGIN, LoginRequest(email, PASSWORD))
        assertEquals(HttpStatusCode.Accepted, login.status)
        return login.body<TwoFactorChallengeResponse>().challengeToken
    }

    @Test
    fun `login asks for a code, and a code works only once`() = testApplication {
        val client = client()
        val (email, secret, _) = client.userWithTwoFactor()
        val challenge = client.challenge(email)

        // The challenge is not an access token.
        assertEquals(HttpStatusCode.Unauthorized, client.get(ApiRoutes.USERS_ME) { bearerAuth(challenge) }.status)

        val wrong = client.postJson(ApiRoutes.AUTH_2FA_VERIFY, TwoFactorLoginRequest(challenge, "000000"))
        assertEquals(ApiErrors.INVALID_TWO_FACTOR_CODE, wrong.body<ErrorResponse>().error)

        // The current code was used to turn 2FA on; the next one (clock drift window) is accepted once.
        val next = codeAt(secret, offsetSteps = 1)
        assertEquals(HttpStatusCode.OK, client.postJson(ApiRoutes.AUTH_2FA_VERIFY, TwoFactorLoginRequest(challenge, next)).status)
        assertEquals(HttpStatusCode.BadRequest, client.postJson(ApiRoutes.AUTH_2FA_VERIFY, TwoFactorLoginRequest(challenge, next)).status)
    }

    @Test
    fun `recovery code works once and can turn 2FA off`() = testApplication {
        val client = client()
        val (email, _, recoveryCodes) = client.userWithTwoFactor()
        assertEquals(10, recoveryCodes.size)

        val ok = client.postJson(ApiRoutes.AUTH_2FA_VERIFY, TwoFactorLoginRequest(client.challenge(email), recoveryCodes[0].lowercase()))
        assertEquals(HttpStatusCode.OK, ok.status)
        val token = ok.body<AuthResponse>().accessToken
        val reused = client.postJson(ApiRoutes.AUTH_2FA_VERIFY, TwoFactorLoginRequest(client.challenge(email), recoveryCodes[0]))
        assertEquals(HttpStatusCode.BadRequest, reused.status)

        val disabled = client.postJson(ApiRoutes.AUTH_2FA_DISABLE, TwoFactorCodeRequest(recoveryCodes[1]), token)
        assertEquals(HttpStatusCode.OK, disabled.status)
        val login = client.postJson(ApiRoutes.AUTH_LOGIN, LoginRequest(email, PASSWORD))
        assertEquals(HttpStatusCode.OK, login.status)
        assertFalse(login.body<AuthResponse>().user.twoFactorEnabled)
    }

    @Test
    fun `too many wrong codes lock 2FA for a while`() = testApplication {
        val client = client()
        val (email, secret, _) = client.userWithTwoFactor()
        val challenge = client.challenge(email)
        repeat(5) { client.postJson(ApiRoutes.AUTH_2FA_VERIFY, TwoFactorLoginRequest(challenge, "000000")) }

        val locked = client.postJson(ApiRoutes.AUTH_2FA_VERIFY, TwoFactorLoginRequest(challenge, codeAt(secret, offsetSteps = 1)))
        assertEquals(HttpStatusCode.TooManyRequests, locked.status)
        assertEquals(ApiErrors.TWO_FACTOR_LOCKED, locked.body<ErrorResponse>().error)
    }

    private companion object {
        const val PASSWORD = "hunter42abc"
    }
}
