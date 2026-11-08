package com.musicroom.app.music

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.player.PlayerController
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TrackResponse

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TrackPickerPage(
    api: MusicRoomApi,
    player: PlayerController,
    title: String,
    addLabel: String,
    addedLabel: String,
    isAdded: (TrackResponse) -> Boolean,
    onAdd: suspend (TrackResponse) -> Unit,
    onBack: () -> Unit,
    showTopTracks: Boolean = true,
) {
    BackHandler(onBack = onBack)
    SubScreenScaffold(title = title, onBack = onBack) {
        TrackPickerContent(api, player, addLabel, addedLabel, isAdded, onAdd, showTopTracks)
    }
}
