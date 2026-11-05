package com.musicroom.app.events

import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.events_filter_invited
import com.musicroom.app.generated.resources.events_filter_nearby
import com.musicroom.app.generated.resources.events_filter_public
import org.jetbrains.compose.resources.StringResource

enum class EventFilter(val label: StringResource) {
    Nearby(Res.string.events_filter_nearby),
    Public(Res.string.events_filter_public),
    Invited(Res.string.events_filter_invited),
}
