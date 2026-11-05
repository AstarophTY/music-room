package com.musicroom.shared.dto

import kotlinx.serialization.Serializable

/** A playlist made in Music Room, as seen by the user who asks for it. */
@Serializable
data class PlaylistResponse(
    val id: String,
    val name: String,
    val type: PlaylistType,
    val owner: PlaylistMemberResponse,
    /** The user owns it: they rename it, change its type, manage members and delete it. */
    val isOwner: Boolean,
    /** The user can add and remove songs and change the photo. */
    val canEdit: Boolean,
    val trackCount: Int,
    /** Members other than the owner. */
    val memberCount: Int,
    /** Changes each time the photo changes (cache key). Null when the playlist has no photo. */
    val coverVersion: Long? = null,
)
