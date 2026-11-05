package com.musicroom.app.player

import com.musicroom.shared.dto.AvatarColor
import com.musicroom.shared.dto.PlaylistMemberResponse
import com.musicroom.shared.dto.UserResponse

/** Who proposed a queued song, enough to show their profile picture. */
data class Proposer(val userId: String, val name: String, val avatarColor: AvatarColor, val avatarVersion: Long?) {
    companion object {
        fun of(user: UserResponse) = Proposer(user.id, user.displayName, user.avatarColor, user.avatarVersion)

        fun of(member: PlaylistMemberResponse) = Proposer(member.userId, member.displayName, member.avatarColor, member.avatarVersion)
    }
}
