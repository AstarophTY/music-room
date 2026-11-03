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
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.decodeToImageBitmap

/** [Avatar] of a user: downloads the photo once (then cached), shows the initials meanwhile or on error. */
@Composable
fun UserAvatar(api: MusicRoomApi, user: UserResponse, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val version = user.avatarVersion
    val key = version?.let { AvatarCache.key(user.id, it) }
    var photo by remember(key) { mutableStateOf(key?.let { AvatarCache[it] }) }
    LaunchedEffect(key) {
        if (key == null || photo != null) return@LaunchedEffect
        photo = try {
            api.avatar(user.id).decodeToImageBitmap().also { AvatarCache[key] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
    Avatar(user.displayName, modifier, size, user.avatarColor, photo)
}
