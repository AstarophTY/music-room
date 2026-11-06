package com.musicroom.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musicroom.app.common.UiText
import com.musicroom.app.common.WindowSize
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.common_retry
import com.musicroom.app.generated.resources.reco_artist
import com.musicroom.app.generated.resources.reco_empty
import com.musicroom.app.generated.resources.reco_top_tracks
import com.musicroom.app.music.TrackCard
import com.musicroom.app.player.PlayerController
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.RecommendationKind
import com.musicroom.shared.dto.RecommendationsResponse
import com.musicroom.shared.dto.TrackResponse
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.stringResource

/** Shelves of tracks from the user's Spotify listening, each a row of big square covers. */
@Composable
fun RecommendationsSection(api: MusicRoomApi, player: PlayerController) {
    val colors = AppTheme.colors
    val size: Dp = if (WindowSize.current != WindowSize.Compact) 192.dp else 164.dp
    var data by remember { mutableStateOf<RecommendationsResponse?>(null) }
    var error by remember { mutableStateOf<UiText?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        error = null
        try {
            data = api.recommendations()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
        }
    }

    val loaded = data
    val failure = error
    when {
        failure != null -> Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            Text(failure.resolve(), style = AppTheme.typography.bodySmall, color = colors.error)
            AppButton(stringResource(Res.string.common_retry), { attempt++ }, variant = AppButtonVariant.Secondary)
        }
        loaded == null -> Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
        loaded.sections.isEmpty() -> Text(stringResource(Res.string.reco_empty), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
        else -> loaded.sections.take(3).forEach { section ->
            val title = when (section.kind) {
                RecommendationKind.TOP_TRACKS -> stringResource(Res.string.reco_top_tracks)
                RecommendationKind.ARTIST -> stringResource(Res.string.reco_artist, section.artist.orEmpty())
            }
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                HomeSectionHeader(title)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                    itemsIndexed(section.tracks, key = { _, track -> track.id }) { index, track ->
                        TrackCard(
                            api,
                            track,
                            playing = player.track?.id == track.id,
                            onClick = { player.play(section.tracks, index) },
                            size = size,
                        )
                    }
                }
            }
        }
    }
}
