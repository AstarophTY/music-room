package com.musicroom.server.modules.auth

import com.musicroom.server.AppModule
import com.musicroom.server.bootstrap
import com.musicroom.server.common.security.PasswordHasher
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
import com.musicroom.shared.dto.RegisterRequest
import com.musicroom.shared.dto.TwoFactorChallengeResponse
import com.musicroom.shared.dto.TwoFactorCodeRequest
import com.musicroom.shared.dto.TwoFactorLoginRequest
import com.musicroom.shared.dto.TwoFactorMethod
import com.musicroom.shared.dto.TwoFactorResendRequest
import com.musicroom.shared.dto.UserResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
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
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@Testcontainers(disabledWithoutDocker = true)
class EmailTwoFactorTest {

    /** Subjects of the emails sent, per address. */
    private val sent = CopyOnWriteArrayList<Pair<String, String>>()

    private val config = AppConfig(
        version = "test",
        database = DatabaseConfig("unused", "unused", "unused"),
        jwt = JwtConfig(secret = "x".repeat(32), issuer = "test", audience = "test", realm = "test"),
    )
    private val appModule = AppModule(
        config,
        TestDatabase.module,
        PasswordHasher(cost = 4),
        emailSender = { to, subject, _ -> sent += to to subject },
    )

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

    /** The 6-digit code of the last email sent to [email]. */
    private fun lastCode(email: String): String =
        Regex("""\d{6}""").find(sent.last { it.first == email }.second)!!.value

    private fun codesSentTo(email: String) = sent.count { it.first == email && Regex("""\d{6}""").containsMatchIn(it.second) }

    private suspend fun HttpClient.register(): Pair<String, String> {
        val email = "user-${UUID.randomUUID()}@example.com"
        val auth = postJson(ApiRoutes.AUTH_REGISTER, RegisterRequest(email, PASSWORD, "Alice", acceptedTerms = true)).body<AuthResponse>()
        return email to auth.accessToken
    }

    private suspend fun HttpClient.enableEmailTwoFactor(email: String, token: String) {
        assertEquals(HttpStatusCode.NoContent, post(ApiRoutes.AUTH_2FA_EMAIL_CODE) { bearerAuth(token) }.status)
        val enabled = postJson(ApiRoutes.AUTH_2FA_EMAIL_ENABLE, TwoFactorCodeRequest(lastCode(email)), token)
        assertEquals(HttpStatusCode.OK, enabled.status)
        assertEquals(TwoFactorMethod.EMAIL, enabled.body<UserResponse>().twoFactorMethod)
    }

    @Test
    fun `email 2FA is off until the user turns it on`() = testApplication {
        val client = client()
        val (email, token) = client.register()
        val login = client.postJson(ApiRoutes.AUTH_LOGIN, LoginRequest(email, PASSWORD))
        assertEquals(HttpStatusCode.OK, login.status)
        assertNull(login.body<AuthResponse>().user.twoFactorMethod)

        client.enableEmailTwoFactor(email, token)
        val challenge = client.postJson(ApiRoutes.AUTH_LOGIN, LoginRequest(email, PASSWORD))
        assertEquals(HttpStatusCode.Accepted, challenge.status)
        assertEquals(TwoFactorMethod.EMAIL, challenge.body<TwoFactorChallengeResponse>().method)
    }

    @Test
    fun `login emails a code that works once`() = testApplication {
        val client = client()
        val (email, token) = client.register()
        client.enableEmailTwoFactor(email, token)

        val challenge = client.postJson(ApiRoutes.AUTH_LOGIN, LoginRequest(email, PASSWORD)).body<TwoFactorChallengeResponse>()
        val code = lastCode(email)
        val wrong = client.postJson(ApiRoutes.AUTH_2FA_VERIFY, TwoFactorLoginRequest(challenge.challengeToken, if (code == "000000") "111111" else "000000"))
        assertEquals(ApiErrors.INVALID_TWO_FACTOR_CODE, wrong.body<ErrorResponse>().error)

        val ok = client.postJson(ApiRoutes.AUTH_2FA_VERIFY, TwoFactorLoginRequest(challenge.challengeToken, code))
        assertEquals(HttpStatusCode.OK, ok.status)
        val reused = client.postJson(ApiRoutes.AUTH_2FA_VERIFY, TwoFactorLoginRequest(challenge.challengeToken, code))
        assertEquals(HttpStatusCode.BadRequest, reused.status)

        // A new code cannot be asked again right away.
        val resend = client.postJson(ApiRoutes.AUTH_2FA_EMAIL_RESEND, TwoFactorResendRequest(challenge.challengeToken))
        assertEquals(HttpStatusCode.TooManyRequests, resend.status)
    }

    @Test
    fun `logging in twice in a row sends one code`() = testApplication {
        val client = client()
        val (email, token) = client.register()
        client.enableEmailTwoFactor(email, token)
        val before = codesSentTo(email)
        client.postJson(ApiRoutes.AUTH_LOGIN, LoginRequest(email, PASSWORD))
        client.postJson(ApiRoutes.AUTH_LOGIN, LoginRequest(email, PASSWORD))
        assertEquals(before + 1, codesSentTo(email))
    }

    @Test
    fun `turning email 2FA off needs an emailed code`() = testApplication {
        val client = client()
        val (email, token) = client.register()
        client.enableEmailTwoFactor(email, token)

        val noCode = client.postJson(ApiRoutes.AUTH_2FA_DISABLE, TwoFactorCodeRequest("123456"), token)
        assertEquals(HttpStatusCode.BadRequest, noCode.status)

        // The enable code is used up; a login emails a fresh one.
        client.postJson(ApiRoutes.AUTH_LOGIN, LoginRequest(email, PASSWORD))
        val disabled = client.postJson(ApiRoutes.AUTH_2FA_DISABLE, TwoFactorCodeRequest(lastCode(email)), token)
        assertEquals(HttpStatusCode.OK, disabled.status)
        assertFalse(disabled.body<UserResponse>().twoFactorEnabled)
        assertEquals(HttpStatusCode.OK, client.postJson(ApiRoutes.AUTH_LOGIN, LoginRequest(email, PASSWORD)).status)
    }

    private companion object {
        const val PASSWORD = "hunter42abc"
    }
}
