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
import com.musicroom.shared.dto.AvatarColor
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * [Avatar] of any user: their photo once downloaded (when [avatarVersion] says they have one), their
 * initials meanwhile. Photos are cached by user and version.
 */
@Composable
fun PersonAvatar(
    api: MusicRoomApi,
    userId: String,
    name: String,
    avatarColor: AvatarColor,
    avatarVersion: Long?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val key = avatarVersion?.let { AvatarCache.key(userId, it) }
    var photo by remember(key) { mutableStateOf(key?.let { AvatarCache[it] }) }
    LaunchedEffect(key) {
        if (key == null || photo != null) return@LaunchedEffect
        photo = try {
            api.avatar(userId).decodeToImageBitmap().also { AvatarCache[key] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
    Avatar(name, modifier, size, avatarColor, photo)
}
