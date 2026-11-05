package com.musicroom.app.events

import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.events_filter_invited
import com.musicroom.app.generated.resources.events_filter_mine
import com.musicroom.app.generated.resources.events_filter_nearby
import com.musicroom.app.generated.resources.events_filter_public
import com.musicroom.shared.dto.EventsScope
import org.jetbrains.compose.resources.StringResource

/** Lists of the events tab, and the server list each one asks for. */
enum class EventFilter(val label: StringResource, val scope: EventsScope) {
    Public(Res.string.events_filter_public, EventsScope.PUBLIC),
    Nearby(Res.string.events_filter_nearby, EventsScope.NEARBY),
    Invited(Res.string.events_filter_invited, EventsScope.INVITED),
    Mine(Res.string.events_filter_mine, EventsScope.MINE),
}
