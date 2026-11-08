package com.musicroom.app.music

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.musicroom.app.player.PlayerController
import com.musicroom.app.theme.components.AppBottomSheet
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TrackResponse

@Composable
fun TrackPickerSheet(
    api: MusicRoomApi,
    player: PlayerController,
    title: String,
    addLabel: String,
    addedLabel: String,
    isAdded: (TrackResponse) -> Boolean,
    onAdd: suspend (TrackResponse) -> Unit,
    onDismiss: () -> Unit,
    showTopTracks: Boolean = true,
) {
    AppBottomSheet(onDismiss) {
        Text(title, style = AppTheme.typography.title, color = AppTheme.colors.textPrimary)
        TrackPickerContent(api, player, addLabel, addedLabel, isAdded, onAdd, showTopTracks)
    }
}
