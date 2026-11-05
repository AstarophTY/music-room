package com.musicroom.app.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.AudioLines
import com.composables.icons.lucide.Guitar
import com.composables.icons.lucide.Headphones
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.Piano
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.X
import com.musicroom.app.common.SectionTitle
import com.musicroom.app.common.TabContent
import com.musicroom.app.common.UiText
import com.musicroom.app.common.WindowSize
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_clear
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.genre_chill
import com.musicroom.app.generated.resources.genre_electro
import com.musicroom.app.generated.resources.genre_hiphop
import com.musicroom.app.generated.resources.genre_jazz
import com.musicroom.app.generated.resources.genre_pop
import com.musicroom.app.generated.resources.genre_rock
import com.musicroom.app.generated.resources.search_browse
import com.musicroom.app.generated.resources.search_load_more
import com.musicroom.app.generated.resources.search_no_results
import com.musicroom.app.generated.resources.search_placeholder
import com.musicroom.app.generated.resources.search_results
import com.musicroom.app.music.SpotifyConnectCard
import com.musicroom.app.music.TrackMoreButton
import com.musicroom.app.music.TrackRow
import com.musicroom.app.player.PlayerController
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Search in the Spotify catalog. Typing searches after a short pause; a mood tile searches its genre.
 * Tapping a result plays it, with the other results queued after it; its "..." (or a swipe to the right) adds it to the queue, the "..." also to a playlist.
 */
@Composable
fun SearchScreen(api: MusicRoomApi, user: UserResponse, player: PlayerController, onSpotifyConnected: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var mood by rememberSaveable { mutableStateOf<Int?>(null) }
    var results by remember { mutableStateOf<List<TrackResponse>>(emptyList()) }
    var nextOffset by remember { mutableStateOf<Int?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors
    // Label, icon, and the Spotify genre it searches.
    val moods = listOf(
        Triple(Res.string.genre_pop, Lucide.Sparkles, "pop"),
        Triple(Res.string.genre_rock, Lucide.Guitar, "rock"),
        Triple(Res.string.genre_hiphop, Lucide.Mic, "hip-hop"),
        Triple(Res.string.genre_electro, Lucide.AudioLines, "electronic"),
        Triple(Res.string.genre_jazz, Lucide.Piano, "jazz"),
        Triple(Res.string.genre_chill, Lucide.Headphones, "chill"),
    )
    val tileColors = listOf(colors.accentSubtle, colors.accentSubtleStrong, colors.surfaceSubtle)
    val selectedMood = mood?.let { moods.getOrNull(it) }
    val searchQuery = selectedMood?.let { "genre:\"${it.third}\"" } ?: query.trim()

    suspend fun load(offset: Int) {
        loading = true
        error = null
        try {
            val page = api.searchTracks(searchQuery, offset)
            results = (if (offset == 0) page.items else results + page.items).distinctBy { it.id }
            nextOffset = page.nextOffset
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
        }
        loading = false
    }

    LaunchedEffect(searchQuery, user.spotifyConnected) {
        results = emptyList()
        nextOffset = null
        error = null
        loading = false
        if (searchQuery.isEmpty() || !user.spotifyConnected) return@LaunchedEffect
        if (selectedMood == null) delay(350)
        load(0)
    }

    TabContent {
        AppTextField(
            query,
            {
                query = it
                mood = null
            },
            stringResource(Res.string.search_placeholder),
            Modifier.fillMaxWidth(),
            leadingIcon = Lucide.Search,
        )
        if (!user.spotifyConnected) {
            SpotifyConnectCard(api, onConnected = onSpotifyConnected)
        } else if (searchQuery.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(
                        selectedMood?.let { stringResource(it.first) } ?: stringResource(Res.string.search_results),
                        Modifier.weight(1f),
                    )
                    if (selectedMood != null) {
                        IconButton(onClick = { mood = null }) {
                            Icon(Lucide.X, contentDescription = stringResource(Res.string.common_clear), tint = colors.textSecondary)
                        }
                    }
                }
                val failure = error
                when {
                    results.isEmpty() && loading ->
                        Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                    results.isEmpty() && failure != null ->
                        Text(failure.resolve(), style = AppTheme.typography.bodySmall, color = colors.error)
                    results.isEmpty() ->
                        Text(stringResource(Res.string.search_no_results), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                    else -> {
                        results.forEachIndexed { index, track ->
                            TrackRow(
                                api,
                                track,
                                playing = player.track?.id == track.id,
                                onClick = { player.play(results, index) },
                                trailing = { TrackMoreButton(api, player, track) },
                                onSwipeRight = { player.addToQueue(listOf(track)) },
                            )
                        }
                        failure?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
                        val offset = nextOffset
                        if (offset != null) {
                            AppButton(
                                stringResource(Res.string.search_load_more),
                                { scope.launch { load(offset) } },
                                Modifier.fillMaxWidth().padding(top = AppTheme.spacing.sm),
                                enabled = !loading,
                                variant = AppButtonVariant.Secondary,
                            )
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
                                    .pressable(enabled = user.spotifyConnected, onClick = { mood = index })
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
                    }
                }
            }
        }
    }
}
