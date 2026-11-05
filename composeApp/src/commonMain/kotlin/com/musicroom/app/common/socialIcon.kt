package com.musicroom.app.common

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.AudioLines
import com.composables.icons.lucide.Cloud
import com.composables.icons.lucide.Gamepad2
import com.composables.icons.lucide.Instagram
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music2
import com.composables.icons.lucide.Twitch
import com.composables.icons.lucide.Twitter
import com.composables.icons.lucide.Youtube
import com.musicroom.shared.dto.SocialNetwork

/** Lucide has no Discord, TikTok, Spotify or SoundCloud logo: those use a close generic icon. */
fun SocialNetwork.icon(): ImageVector = when (this) {
    SocialNetwork.INSTAGRAM -> Lucide.Instagram
    SocialNetwork.TIKTOK -> Lucide.Music2
    SocialNetwork.X -> Lucide.Twitter
    SocialNetwork.YOUTUBE -> Lucide.Youtube
    SocialNetwork.TWITCH -> Lucide.Twitch
    SocialNetwork.DISCORD -> Lucide.Gamepad2
    SocialNetwork.SPOTIFY -> Lucide.AudioLines
    SocialNetwork.SOUNDCLOUD -> Lucide.Cloud
}
