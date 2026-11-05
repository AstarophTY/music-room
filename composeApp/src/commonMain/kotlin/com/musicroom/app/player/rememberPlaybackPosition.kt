package com.musicroom.app.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay

/** Position in the current track (ms), moving forward twice a second while it plays. */
@Composable
fun rememberPlaybackPosition(player: PlayerController): State<Long> =
    produceState(player.positionMs(), player.state) {
        while (true) {
            value = player.positionMs()
            delay(500)
        }
    }
