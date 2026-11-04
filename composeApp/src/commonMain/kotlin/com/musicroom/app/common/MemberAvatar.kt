package com.musicroom.app.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PlaylistMemberResponse
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.decodeToImageBitmap

/** [Avatar] of a playlist member, like [UserAvatar]: the photo once downloaded, the initials meanwhile. */
@Composable
fun MemberAvatar(api: MusicRoomApi, member: PlaylistMemberResponse, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val key = member.avatarVersion?.let { AvatarCache.key(member.userId, it) }
    var photo by remember(key) { mutableStateOf(key?.let { AvatarCache[it] }) }
    LaunchedEffect(key) {
        if (key == null || photo != null) return@LaunchedEffect
        photo = try {
            api.avatar(member.userId).decodeToImageBitmap().also { AvatarCache[key] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
    Avatar(member.displayName, modifier, size, member.avatarColor, photo)
}
