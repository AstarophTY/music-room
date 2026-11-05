package com.musicroom.app.player

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Car
import com.composables.icons.lucide.Laptop
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MonitorSpeaker
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Speaker
import com.composables.icons.lucide.Tablet
import com.composables.icons.lucide.Tv
import com.musicroom.shared.dto.DeviceType

/** Icon of a kind of Spotify device. */
fun DeviceType.icon(): ImageVector = when (this) {
    DeviceType.COMPUTER -> Lucide.Laptop
    DeviceType.SMARTPHONE -> Lucide.Smartphone
    DeviceType.TABLET -> Lucide.Tablet
    DeviceType.SPEAKER -> Lucide.Speaker
    DeviceType.TV -> Lucide.Tv
    DeviceType.CAR -> Lucide.Car
    DeviceType.OTHER -> Lucide.MonitorSpeaker
}
