package com.musicroom.app.search

import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.search_filter_albums
import com.musicroom.app.generated.resources.search_filter_all
import com.musicroom.app.generated.resources.search_filter_artists
import com.musicroom.app.generated.resources.search_filter_tracks
import org.jetbrains.compose.resources.StringResource

/** Which results the search shows: a bit of each kind, or one kind only. */
enum class SearchFilter(val label: StringResource) {
    All(Res.string.search_filter_all),
    Tracks(Res.string.search_filter_tracks),
    Artists(Res.string.search_filter_artists),
    Albums(Res.string.search_filter_albums),
}
