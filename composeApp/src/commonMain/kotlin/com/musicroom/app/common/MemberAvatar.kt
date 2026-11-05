package com.musicroom.app.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PlaylistMemberResponse

/** [Avatar] of a playlist member, like [UserAvatar]: the photo once downloaded, the initials meanwhile. */
@Composable
fun MemberAvatar(api: MusicRoomApi, member: PlaylistMemberResponse, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    PersonAvatar(api, member.userId, member.displayName, member.avatarColor, member.avatarVersion, modifier, size)
}
