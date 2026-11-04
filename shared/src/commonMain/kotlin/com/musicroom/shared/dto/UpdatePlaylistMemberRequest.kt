package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePlaylistMemberRequest(val access: PlaylistAccess)
