package com.musicroom.app.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.UserResponse

/** [Avatar] of a user: downloads the photo once (then cached), shows the initials meanwhile or on error. */
@Composable
fun UserAvatar(api: MusicRoomApi, user: UserResponse, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    PersonAvatar(api, user.id, user.displayName, user.avatarColor, user.avatarVersion, modifier, size)
}
