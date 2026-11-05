package com.musicroom.app.social

import androidx.compose.runtime.Composable
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.notification_a_playlist
import com.musicroom.app.generated.resources.notification_friend_accepted
import com.musicroom.app.generated.resources.notification_friend_request
import com.musicroom.app.generated.resources.notification_playlist_invite
import com.musicroom.app.generated.resources.notification_someone
import com.musicroom.shared.dto.NotificationResponse
import com.musicroom.shared.dto.NotificationType
import org.jetbrains.compose.resources.stringResource

/** What [item] says, e.g. "Sam wants to be your friend". */
@Composable
fun notificationText(item: NotificationResponse): String {
    val name = item.actor?.displayName ?: stringResource(Res.string.notification_someone)
    return when (item.type) {
        NotificationType.FRIEND_REQUEST -> stringResource(Res.string.notification_friend_request, name)
        NotificationType.FRIEND_ACCEPTED -> stringResource(Res.string.notification_friend_accepted, name)
        NotificationType.PLAYLIST_INVITE -> stringResource(
            Res.string.notification_playlist_invite,
            name,
            item.playlistName ?: stringResource(Res.string.notification_a_playlist),
        )
    }
}
