package com.musicroom.app.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.AudioLines
import com.composables.icons.lucide.Coffee
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Guitar
import com.composables.icons.lucide.Headphones
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.Music2
import com.composables.icons.lucide.Piano
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Sun
import com.composables.icons.lucide.TreePine
import com.composables.icons.lucide.Waves
import com.composables.icons.lucide.X
import com.musicroom.app.common.SectionTitle
import com.musicroom.app.common.TabContent
import com.musicroom.app.common.UiText
import com.musicroom.app.common.WindowSize
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_clear
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.apple_music_storefront_missing
import com.musicroom.app.generated.resources.genre_chill
import com.musicroom.app.generated.resources.genre_classical
import com.musicroom.app.generated.resources.genre_country
import com.musicroom.app.generated.resources.genre_electro
import com.musicroom.app.generated.resources.genre_hiphop
import com.musicroom.app.generated.resources.genre_indie
import com.musicroom.app.generated.resources.genre_jazz
import com.musicroom.app.generated.resources.genre_latin
import com.musicroom.app.generated.resources.genre_metal
import com.musicroom.app.generated.resources.genre_pop
import com.musicroom.app.generated.resources.genre_reggae
import com.musicroom.app.generated.resources.genre_rnb
import com.musicroom.app.generated.resources.genre_rock
import com.musicroom.app.generated.resources.genre_soul
import com.musicroom.app.generated.resources.search_browse
import com.musicroom.app.generated.resources.search_load_more
import com.musicroom.app.generated.resources.search_no_people
import com.musicroom.app.generated.resources.search_no_results
import com.musicroom.app.generated.resources.search_placeholder
import com.musicroom.app.generated.resources.search_see_all
import com.musicroom.app.generated.resources.tab_search
import com.musicroom.app.music.AlbumCard
import com.musicroom.app.music.ArtistCard
import com.musicroom.app.music.TrackMoreButton
import com.musicroom.app.music.TrackRow
import com.musicroom.app.music.AppleMusicConnection
import com.musicroom.app.player.PlayerController
import com.musicroom.app.social.FriendButton
import com.musicroom.app.social.PersonRow
import com.musicroom.app.social.SocialController
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppChip
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.PublicUserResponse
import com.musicroom.shared.dto.SearchResponse
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.compose.resources.stringResource

/**
 * Search in the Spotify catalog (songs, artists and albums) and among Music Room users by name. Typing searches
 * after a short pause; a mood tile searches its genre. "All" shows a few of each kind, the chips one kind only
 * (songs page by page). Everything can be searched without a Spotify account (playing a song asks to link one);
 * tapping a person opens their profile ([onOpenUser]).
 * Tapping a song plays it with the other songs queued after it; its "..." or a swipe to the right adds it to
 * the queue (or proposes it to the focused event). Artists and albums open their page in place of the results; back returns to them.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    api: MusicRoomApi,
    user: UserResponse,
    player: PlayerController,
    social: SocialController,
    onOpenUser: (String) -> Unit,
    onDetailOpenChange: (Boolean) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var mood by rememberSaveable { mutableStateOf<Int?>(null) }
    var filter by rememberSaveable { mutableStateOf(SearchFilter.All) }
    var found by remember { mutableStateOf<SearchResponse?>(null) }
    var tracks by remember { mutableStateOf<List<TrackResponse>>(emptyList()) }
    var nextOffset by remember { mutableStateOf<Int?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }
    // Music Room users whose name matches; null while not searched.
    var people by remember { mutableStateOf<List<PublicUserResponse>?>(null) }
    var peopleError by remember { mutableStateOf<UiText?>(null) }
    // Artist and album pages opened from the results, the last one on top: "artist|<id>|<name>".
    var pages by rememberSaveable { mutableStateOf(listOf<String>()) }
    var opening by remember { mutableStateOf(true) }
    val artistPage = "artist"
    val albumPage = "album"
    val moodPage = "mood"
    val resultsPage = "results"
    SideEffect {
        val current = pages.lastOrNull()
        onDetailOpenChange(current?.startsWith("$artistPage|") == true || current?.startsWith("$albumPage|") == true)
    }
    // Songs shown under "All" before "See all".
    val topTrackCount = 5
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors
    val appleMusic = AppleMusicConnection.Local.current
    val compact = WindowSize.current == WindowSize.Compact
    // Label, icon, and the Spotify genre it searches.
    val moods = listOf(
        Triple(Res.string.genre_pop, Lucide.Sparkles, "pop"),
        Triple(Res.string.genre_rock, Lucide.Guitar, "rock"),
        Triple(Res.string.genre_hiphop, Lucide.Mic, "hip-hop"),
        Triple(Res.string.genre_electro, Lucide.AudioLines, "electronic"),
        Triple(Res.string.genre_jazz, Lucide.Piano, "jazz"),
        Triple(Res.string.genre_chill, Lucide.Headphones, "chill"),
        Triple(Res.string.genre_rnb, Lucide.Heart, "r-n-b"),
        Triple(Res.string.genre_latin, Lucide.Sun, "latin"),
        Triple(Res.string.genre_classical, Lucide.Music2, "classical"),
        Triple(Res.string.genre_reggae, Lucide.Waves, "reggae"),
        Triple(Res.string.genre_country, Lucide.TreePine, "country"),
        Triple(Res.string.genre_metal, Lucide.Flame, "metal"),
        Triple(Res.string.genre_indie, Lucide.Star, "indie"),
        Triple(Res.string.genre_soul, Lucide.Coffee, "soul"),
    )
    val tileColors = listOf(colors.accentSubtle, colors.accentSubtleStrong, colors.surfaceSubtle)
    val selectedMood = mood?.let { moods.getOrNull(it) }
    val searchQuery = selectedMood?.let { "genre:\"${it.third}\"" } ?: query.trim()
    // A mood is a Spotify genre: no one to find.
    val peopleQuery = if (selectedMood == null) query.trim() else ""
    // Topped by the user with exactly this name (names are unique).
    val topPeopleCount = 3

    LaunchedEffect(searchQuery, appleMusic.connected, appleMusic.storefront, user.spotifyConnected) {
        found = null
        tracks = emptyList()
        nextOffset = null
        error = null
        loading = false
        if (searchQuery.isEmpty()) return@LaunchedEffect
        if (selectedMood == null) delay(350)
        loading = true
        try {
            coroutineScope {
                val useSpotify = user.spotifyConnected
                if (appleMusic.connected && appleMusic.storefront.isNullOrBlank()) {
                    error = UiText.of(Res.string.apple_music_storefront_missing)
                }
                suspend fun <T> attempt(block: suspend () -> T): kotlin.Result<T> = try {
                    kotlin.Result.success(block())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    kotlin.Result.failure(e)
                }
                val spotifyTask = if (useSpotify) async { attempt { api.search(searchQuery) } } else null
                val appleTask = appleMusic.storefront?.takeIf { appleMusic.connected }?.let { region ->
                    async { attempt { api.searchAppleMusic(selectedMood?.third ?: searchQuery, region) } }
                }
                val spotifyResult = spotifyTask?.await()
                val appleResult = appleTask?.await()
                val spotifyData = spotifyResult?.getOrNull() ?: SearchResponse()
                val appleData = appleResult?.getOrNull() ?: SearchResponse()
                val mixed = buildList {
                    for (index in 0 until maxOf(spotifyData.tracks.size, appleData.tracks.size)) {
                        spotifyData.tracks.getOrNull(index)?.let(::add)
                        appleData.tracks.getOrNull(index)?.let(::add)
                    }
                }
                val failure = spotifyResult?.exceptionOrNull() ?: appleResult?.exceptionOrNull()
                if (failure is Exception) error = UiText.of(failure)
                if ((spotifyResult == null || spotifyResult.isFailure) && (appleResult == null || appleResult.isFailure)) {
                    found = null
                } else {
                    found = SearchResponse(
                        tracks = mixed,
                        artists = spotifyData.artists + appleData.artists,
                        albums = spotifyData.albums + appleData.albums,
                        nextTrackOffset = spotifyData.nextTrackOffset,
                    )
                    tracks = mixed
                    nextOffset = spotifyData.nextTrackOffset
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
        }
        loading = false
    }

    LaunchedEffect(peopleQuery) {
        people = null
        peopleError = null
        if (peopleQuery.isEmpty()) return@LaunchedEffect
        delay(350)
        try {
            people = api.searchUsers(peopleQuery).items
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            peopleError = UiText.of(e)
        }
    }

    fun loadMoreTracks(offset: Int) {
        scope.launch {
            loading = true
            error = null
            try {
                val page = api.searchTracks(searchQuery, offset)
                tracks = (tracks + page.items).distinctBy { it.id }
                nextOffset = page.nextOffset
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
            }
            loading = false
        }
    }

    fun open(kind: String, id: String, name: String) {
        opening = true
        pages = pages + "$kind|$id|$name"
    }

    fun back() {
        opening = false
        if (pages.lastOrNull()?.startsWith("$moodPage|") == true) mood = null
        pages = pages.dropLast(1)
    }
    BackHandler(enabled = pages.isNotEmpty()) { back() }

    // Section title with "See all", which switches to that kind only.
    val sectionHeader: @Composable (String, SearchFilter?) -> Unit = { title, seeAll ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(title, Modifier.weight(1f))
            if (seeAll != null) {
                Text(
                    stringResource(Res.string.search_see_all),
                    style = AppTheme.typography.label,
                    color = colors.accentText,
                    modifier = Modifier.pressable(onClick = { filter = seeAll }).padding(AppTheme.spacing.xs),
                )
            }
        }
    }
    // Cards [columns] per row, the last row padded so every card keeps the same width.
    val grid: @Composable (Int, Int, @Composable (Int, Modifier) -> Unit) -> Unit = { count, columns, card ->
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)) {
            (0 until count).chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                    row.forEach { card(it, Modifier.weight(1f)) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    val trackRows: @Composable (List<TrackResponse>) -> Unit = { shown ->
        Column {
            shown.forEachIndexed { index, track ->
                TrackRow(
                    api,
                    track,
                    playing = player.track?.id == track.id,
                    onClick = { player.play(tracks, index) },
                    trailing = { TrackMoreButton(api, player, track) },
                    onSwipeRight = { player.addToQueue(listOf(track)) },
                )
            }
        }
    }

    // People found, each with the friendship button; the status changes in place.
    val peopleRows: @Composable (List<PublicUserResponse>) -> Unit = { shown ->
        Column {
            shown.forEach { person ->
                PersonRow(api, person, onClick = { onOpenUser(person.id) }) {
                    FriendButton(
                        social,
                        person,
                        onChange = { status -> people = people?.map { if (it.id == person.id) it.copy(friendship = status) else it } },
                        onError = { peopleError = it },
                    )
                }
            }
        }
    }
    val peopleResults: @Composable () -> Unit = {
        val found = people
        val failure = peopleError
        when {
            failure != null -> Text(failure.resolve(), style = AppTheme.typography.bodySmall, color = colors.error)
            found == null -> Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            found.isEmpty() -> Text(stringResource(Res.string.search_no_people), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            else -> peopleRows(found)
        }
    }

    val results: @Composable () -> Unit = {
        TabContent {
            if (!compact) {
                Text(stringResource(Res.string.tab_search), style = AppTheme.typography.display, color = colors.textPrimary)
            }
            AppTextField(
                query,
                {
                    query = it
                    if (mood != null) back()
                },
                stringResource(Res.string.search_placeholder),
                Modifier.fillMaxWidth(),
                leadingIcon = Lucide.Search,
            )
            if (searchQuery.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                        ) {
                            SearchFilter.entries.filter { it != SearchFilter.People || peopleQuery.isNotEmpty() }.forEach {
                                AppChip(stringResource(it.label), selected = filter == it, onClick = { filter = it })
                            }
                        }
                        if (selectedMood != null) {
                            IconButton(onClick = { back() }) {
                                Icon(Lucide.X, contentDescription = stringResource(Res.string.common_clear), tint = colors.textSecondary)
                            }
                        }
                    }
                    val result = found
                    val failure = error
                    val foundPeople = people.orEmpty()
                    when {
                        filter == SearchFilter.People -> peopleResults()
                        result == null && loading ->
                            Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                        result == null && failure != null ->
                            Text(failure.resolve(), style = AppTheme.typography.bodySmall, color = colors.error)
                        result == null -> Unit
                        tracks.isEmpty() && result.artists.isEmpty() && result.albums.isEmpty() && foundPeople.isEmpty() ->
                            Text(stringResource(Res.string.search_no_results), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                        else -> AnimatedContent(
                            filter,
                            transitionSpec = { fadeIn(tween(200, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
                        ) { shown ->
                            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)) {
                                if (shown != SearchFilter.Tracks && failure != null) {
                                    Text(failure.resolve(), style = AppTheme.typography.bodySmall, color = colors.error)
                                }
                                when (shown) {
                                    SearchFilter.All -> {
                                        if (foundPeople.isNotEmpty()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                                                sectionHeader(
                                                    stringResource(SearchFilter.People.label),
                                                    SearchFilter.People.takeIf { foundPeople.size > topPeopleCount },
                                                )
                                                peopleRows(foundPeople.take(topPeopleCount))
                                            }
                                        }
                                        if (tracks.isNotEmpty()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                                                sectionHeader(stringResource(SearchFilter.Tracks.label), SearchFilter.Tracks.takeIf { tracks.size > topTrackCount })
                                                trackRows(tracks.take(topTrackCount))
                                            }
                                        }
                                        if (result.artists.isNotEmpty()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                                                sectionHeader(stringResource(SearchFilter.Artists.label), SearchFilter.Artists)
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                                                    items(result.artists, key = { it.id }) { artist ->
                                                        ArtistCard(api, artist, { open(artistPage, artist.id, artist.name) }, Modifier.width(if (compact) 116.dp else 140.dp))
                                                    }
                                                }
                                            }
                                        }
                                        if (result.albums.isNotEmpty()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                                                sectionHeader(stringResource(SearchFilter.Albums.label), SearchFilter.Albums)
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                                                    items(result.albums, key = { it.id }) { album ->
                                                        AlbumCard(api, album, { open(albumPage, album.id, album.name) }, Modifier.width(if (compact) 140.dp else 168.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    SearchFilter.Tracks -> Column {
                                        trackRows(tracks)
                                        failure?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
                                        val offset = nextOffset
                                        if (offset != null) {
                                            AppButton(
                                                stringResource(Res.string.search_load_more),
                                                { loadMoreTracks(offset) },
                                                Modifier.fillMaxWidth().padding(top = AppTheme.spacing.sm),
                                                enabled = !loading,
                                                variant = AppButtonVariant.Secondary,
                                            )
                                        }
                                    }
                                    SearchFilter.Artists -> grid(result.artists.size, if (compact) 3 else 5) { index, modifier ->
                                        val artist = result.artists[index]
                                        ArtistCard(api, artist, { open(artistPage, artist.id, artist.name) }, modifier)
                                    }
                                    // Shown above, outside the Spotify results.
                                    SearchFilter.People -> Unit
                                    SearchFilter.Albums -> grid(result.albums.size, if (compact) 2 else 4) { index, modifier ->
                                        val album = result.albums[index]
                                        AlbumCard(api, album, { open(albumPage, album.id, album.name) }, modifier)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (searchQuery.isEmpty()) {
                SectionTitle(stringResource(Res.string.search_browse))
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    val columns = if (WindowSize.current == WindowSize.Compact) 2 else 3
                    moods.indices.chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                            row.forEach { index ->
                                val (label, icon) = moods[index]
                                Box(
                                    Modifier.weight(1f).height(96.dp)
                                        .pressable(onClick = {
                                            mood = index
                                            open(moodPage, index.toString(), "")
                                        })
                                        .background(tileColors[index % tileColors.size], AppShapes.large)
                                        .padding(AppTheme.spacing.md),
                                ) {
                                    Text(stringResource(label), style = AppTheme.typography.heading, color = colors.textPrimary)
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = colors.accentText,
                                        modifier = Modifier.align(Alignment.BottomEnd).size(32.dp),
                                    )
                                }
                            }
                            // Keep the tiles of a short last row the same width as the others.
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }

    // The results, or the artist or album opened from them (a new page slides in from the right).
    AnimatedContent(
        pages.lastOrNull() ?: resultsPage,
        transitionSpec = {
            (slideInHorizontally(tween(280)) { if (opening) it / 2 else -it / 4 } + fadeIn(tween(280)))
                .togetherWith(slideOutHorizontally(tween(220)) { if (opening) -it / 4 else it / 2 } + fadeOut(tween(180)))
        },
    ) { page ->
        val (kind, id, name) = page.split("|", limit = 3).let { Triple(it[0], it.getOrElse(1) { "" }, it.getOrElse(2) { "" }) }
        when (kind) {
            artistPage -> ArtistScreen(api, id, name, player, onBack = ::back, onOpenAlbum = { albumId, albumName -> open(albumPage, albumId, albumName) })
            albumPage -> AlbumScreen(api, id, name, player, onBack = ::back, onOpenArtist = { artistId, artistName -> open(artistPage, artistId, artistName) })
            else -> results()
        }
    }
}
