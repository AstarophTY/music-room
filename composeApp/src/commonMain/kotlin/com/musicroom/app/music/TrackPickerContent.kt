package com.musicroom.app.music

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.CirclePlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.musicroom.app.common.SectionTitle
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.search_placeholder
import com.musicroom.app.generated.resources.song_suggestions
import com.musicroom.app.generated.resources.search_no_results
import com.musicroom.app.generated.resources.provider_catalog_unavailable
import com.musicroom.app.music.MusicProviderConnection
import com.musicroom.app.music.ProviderSession
import com.musicroom.app.player.PlayerController
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TrackResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Shows suggested songs and searches available music providers.
 * The + of a song runs [onAdd] and turns into a check mark once [isAdded] says so. Tapping a song plays it.
 */
@Composable
fun TrackPickerContent(
    api: MusicRoomApi,
    player: PlayerController,
    addLabel: String,
    addedLabel: String,
    isAdded: (TrackResponse) -> Boolean,
    onAdd: suspend (TrackResponse) -> Unit,
    connections: List<MusicProviderConnection> = ProviderSession.connections.current,
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<TrackResponse>?>(null) }
    var adding by remember { mutableStateOf<Set<String>>(emptySet()) }
    var error by remember { mutableStateOf<UiText?>(null) }
    val providerState = connections.map { Triple(it.source, it.connected, it.searchable) }
    val connected = connections.any { it.available && it.connected }
    var choosingProvider by remember { mutableStateOf(!connected) }
    val onProviderChanged = ProviderSession.onProviderChanged.current

    LaunchedEffect(query, choosingProvider, providerState) {
        results = null
        error = null
        if (choosingProvider) return@LaunchedEffect
        if (connections.any { it.connected && !it.searchable }) {
            error = UiText.of(Res.string.provider_catalog_unavailable)
        }
        val text = query.trim()
        if (text.isNotEmpty()) delay(350)
        results = try {
            val catalog = MusicProviderRegistry(connections).catalogSearch()
            val result = if (text.isEmpty()) catalog.suggestions() else catalog.tracks(text)
            result.failures.firstOrNull()?.let { error = UiText.of(it) }
            result.response?.tracks.orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
            emptyList()
        }
    }

    fun add(track: TrackResponse) {
        if (isAdded(track) || track.id in adding) return
        adding = adding + track.id
        error = null
        scope.launch {
            try {
                onAdd(track)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
            }
            adding = adding - track.id
        }
    }

    run {
        if (choosingProvider) {
            ProviderChoice(
                onProviderChanged = { choosingProvider = false; onProviderChanged() },
                onContinue = { choosingProvider = false },
                listenerOption = false,
            )
        } else {
        AppTextField(query, { query = it }, stringResource(Res.string.search_placeholder), Modifier.fillMaxWidth(), leadingIcon = Lucide.Search)
        if (query.isBlank() && connected) SectionTitle(stringResource(Res.string.song_suggestions))
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
        val loaded = results
        when {
            loaded == null -> Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            loaded.isEmpty() && query.isBlank() -> Unit
            loaded.isEmpty() -> Text(stringResource(Res.string.search_no_results), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            else -> loaded.forEachIndexed { index, track ->
                val added = isAdded(track)
                TrackRow(
                    api,
                    track,
                    playing = player.track?.id == track.id,
                    onClick = { player.play(loaded, index) },
                    trailing = {
                        IconButton(onClick = { add(track) }, enabled = !added && track.id !in adding) {
                            AnimatedContent(added, transitionSpec = { scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut() }) { done ->
                                Icon(
                                    if (done) Lucide.CircleCheck else Lucide.CirclePlus,
                                    contentDescription = if (done) addedLabel else addLabel,
                                    tint = if (done) colors.accentText else colors.textPrimary,
                                )
                            }
                        }
                    },
                )
            }
        }
        }
    }
}
