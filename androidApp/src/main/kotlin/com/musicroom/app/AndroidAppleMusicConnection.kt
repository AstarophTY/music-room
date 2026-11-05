package com.musicroom.app

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.apple.android.sdk.authentication.AuthenticationFactory
import com.musicroom.app.music.AppleMusicConnection
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidAppleMusicConnection(activity: ComponentActivity) : AppleMusicConnection {
    private val manager = AuthenticationFactory.createAuthenticationManager(activity)
    private var pending: Continuation<Boolean>? = null
    private var userToken: String? = null
    override val available = true
    override var connected by mutableStateOf(false)
        private set

    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val continuation = pending ?: return@registerForActivityResult
        pending = null
        val data = result.data
        if (data == null) {
            continuation.resume(false)
        } else {
            val token = manager.handleTokenResult(data)
            if (token.isError) {
                continuation.resumeWithException(IllegalStateException("Apple Music authorization failed: ${token.error}"))
            } else {
                userToken = token.musicUserToken
                connected = !userToken.isNullOrBlank()
                continuation.resume(connected)
            }
        }
    }

    override suspend fun connect(developerToken: String): Boolean = suspendCancellableCoroutine { continuation ->
        check(pending == null) { "Apple Music authorization is already in progress" }
        pending = continuation
        continuation.invokeOnCancellation { if (pending === continuation) pending = null }
        try {
            launcher.launch(manager.createIntentBuilder(developerToken).build())
        } catch (error: Exception) {
            pending = null
            continuation.resumeWithException(error)
        }
    }

    override fun disconnect() {
        userToken = null
        connected = false
    }
}
