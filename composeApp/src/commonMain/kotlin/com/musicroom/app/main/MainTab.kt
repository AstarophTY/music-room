package com.musicroom.app.main

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.CirclePlus
import com.composables.icons.lucide.Disc3
import com.composables.icons.lucide.House
import com.composables.icons.lucide.ListMusic
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.tab_create
import com.musicroom.app.generated.resources.tab_events
import com.musicroom.app.generated.resources.tab_home
import com.musicroom.app.generated.resources.tab_playlists
import com.musicroom.app.generated.resources.tab_search
import org.jetbrains.compose.resources.StringResource

/** Bottom bar tabs, in display order. [Events] sits in the middle as the main action. */
enum class MainTab(val label: StringResource, val icon: ImageVector) {
    Home(Res.string.tab_home, Lucide.House),
    Search(Res.string.tab_search, Lucide.Search),
    Events(Res.string.tab_events, Lucide.Disc3),
    Playlists(Res.string.tab_playlists, Lucide.ListMusic),
    Create(Res.string.tab_create, Lucide.CirclePlus),
}
