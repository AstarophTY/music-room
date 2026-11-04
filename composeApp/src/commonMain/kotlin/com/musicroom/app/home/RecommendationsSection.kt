package com.musicroom.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.musicroom.app.common.SectionTitle
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.common_retry
import com.musicroom.app.generated.resources.reco_artist
import com.musicroom.app.generated.resources.reco_empty
import com.musicroom.app.generated.resources.reco_title
import com.musicroom.app.generated.resources.reco_top_tracks
import com.musicroom.app.player.PlayerController
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.RecommendationKind
import com.musicroom.shared.dto.RecommendationsResponse
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.stringResource

/** "For you": rows of tracks from the user's Spotify listening, one [TrackShelf] per section. */
@Composable
fun RecommendationsSection(api: MusicRoomApi, player: PlayerController) {
    val colors = AppTheme.colors
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

    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
        SectionTitle(stringResource(Res.string.reco_title))
        val loaded = data
        val failure = error
        when {
            failure != null -> {
                Text(failure.resolve(), style = AppTheme.typography.bodySmall, color = colors.error)
                AppButton(stringResource(Res.string.common_retry), { attempt++ }, variant = AppButtonVariant.Secondary)
            }
            loaded == null -> Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            loaded.sections.isEmpty() -> Text(stringResource(Res.string.reco_empty), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            else -> loaded.sections.forEach { section ->
                val title = when (section.kind) {
                    RecommendationKind.TOP_TRACKS -> stringResource(Res.string.reco_top_tracks)
                    RecommendationKind.ARTIST -> stringResource(Res.string.reco_artist, section.artist.orEmpty())
                }
                TrackShelf(title, section.tracks, api, player)
            }
        }
    }
}
