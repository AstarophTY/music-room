package com.musicroom.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.musicroom.app.music.AppleMusicConnection
import com.musicroom.app.music.AppleMusicException
import com.musicroom.shared.MusicRoomApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSUserDefaults
import platform.StoreKit.SKCloudServiceAuthorizationStatus
import platform.StoreKit.SKCloudServiceController
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Links the Apple Music account of the device, with StoreKit.
 *
 * The user token it asks Apple for is what proves the account is linked; playback itself goes through the
 * Music app and needs no token ([IosAppleMusicPlayback]). StoreKit only tells the region in three letters
 * ("FRA"), while the catalog wants two ("fr"), so the server resolves it from the user token.
 */
class IosAppleMusicConnection(private val api: MusicRoomApi) : AppleMusicConnection {
    private val defaults = NSUserDefaults.standardUserDefaults
    private var userToken: String? = defaults.stringForKey(TOKEN_KEY)?.takeIf { it.isNotBlank() }
    override val available = true
    override var storefront: String? = defaults.stringForKey(STOREFRONT_KEY)?.takeIf { it.isNotBlank() }
        private set
    override var connected by mutableStateOf(!userToken.isNullOrBlank())
        private set

    override suspend fun connect(developerToken: String): Boolean {
        // The prompt only shows the first time; a refusal comes straight back on the next attempts.
        if (requestAuthorization() != SKCloudServiceAuthorizationStatus.SKCloudServiceAuthorizationStatusAuthorized) return false
        val token = requestUserToken(developerToken) ?: return false
        // Fetched before anything is kept: a connection without a region could not search the catalog.
        val region = api.appleMusicStorefront(token).storefront
        defaults.setObject(token, forKey = TOKEN_KEY)
        defaults.setObject(region, forKey = STOREFRONT_KEY)
        userToken = token
        storefront = region
        connected = true
        return true
    }

    override fun disconnect() {
        defaults.removeObjectForKey(TOKEN_KEY)
        defaults.removeObjectForKey(STOREFRONT_KEY)
        userToken = null
        storefront = null
        connected = false
    }

    private suspend fun requestAuthorization(): SKCloudServiceAuthorizationStatus =
        suspendCancellableCoroutine { continuation ->
            SKCloudServiceController.requestAuthorization { status -> continuation.resume(status) }
        }

    private suspend fun requestUserToken(developerToken: String): String? =
        suspendCancellableCoroutine { continuation ->
            SKCloudServiceController().requestUserTokenForDeveloperToken(developerToken) { token, error ->
                if (error != null) {
                    continuation.resumeWithException(AppleMusicException(error.localizedDescription))
                } else {
                    continuation.resume(token?.takeIf { it.isNotBlank() })
                }
            }
        }

    private companion object {
        const val TOKEN_KEY = "apple_music_user_token"
        const val STOREFRONT_KEY = "apple_music_storefront"
    }
}
