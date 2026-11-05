package com.musicroom.app.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ListPlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MicVocal
import com.composables.icons.lucide.Play
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.common.Reveal
import com.musicroom.app.common.SectionTitle
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.artist_albums
import com.musicroom.app.generated.resources.artist_label
import com.musicroom.app.generated.resources.artist_no_tracks
import com.musicroom.app.generated.resources.artist_popular
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.common_retry
import com.musicroom.app.generated.resources.playlists_play
import com.musicroom.app.generated.resources.queue_add
import com.musicroom.app.music.AlbumCard
import com.musicroom.app.music.TrackMoreButton
import com.musicroom.app.music.TrackRow
import com.musicroom.app.player.PlayerController
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.ArtistDetailResponse
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.stringResource

/**
 * An artist: round photo and name, Play and Add to queue for their popular songs, the songs (tap to play
 * from one, swipe or "..." to queue), then their albums and singles ([onOpenAlbum]).
 */
@Composable
fun ArtistScreen(
    api: MusicRoomApi,
    artistId: String,
    title: String,
    player: PlayerController,
    onBack: () -> Unit,
    onOpenAlbum: (String, String) -> Unit,
) {
    val colors = AppTheme.colors
    var detail by remember(artistId) { mutableStateOf<ArtistDetailResponse?>(null) }
    var error by remember(artistId) { mutableStateOf<UiText?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(artistId, attempt) {
        error = null
        try {
            detail = api.artist(artistId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
        }
    }

    SubScreenScaffold(title = detail?.artist?.name ?: title, onBack = onBack) {
        val loaded = detail
        if (loaded == null) {
            val failure = error
            if (failure != null) {
                Text(failure.resolve(), style = AppTheme.typography.bodySmall, color = colors.error)
                AppButton(stringResource(Res.string.common_retry), { attempt++ }, variant = AppButtonVariant.Secondary)
            } else {
                Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            }
            return@SubScreenScaffold
        }
        val artist = loaded.artist
        val tracks = loaded.tracks

        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            Box(Modifier.size(160.dp).clip(CircleShape).background(colors.surfaceSubtle), contentAlignment = Alignment.Center) {
                if (artist.imageUrl != null) {
                    RemoteImage(api, artist.imageUrl, Modifier.fillMaxSize())
                } else {
                    Icon(Lucide.MicVocal, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(56.dp))
                }
            }
            Text(artist.name, style = AppTheme.typography.heading, color = colors.textPrimary)
            Text(stringResource(Res.string.artist_label), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                AppButton(stringResource(Res.string.playlists_play), { player.play(tracks, 0) }, enabled = tracks.isNotEmpty(), icon = Lucide.Play)
                IconButton(onClick = { player.addToQueue(tracks) }, enabled = tracks.isNotEmpty()) {
                    Icon(Lucide.ListPlus, contentDescription = stringResource(Res.string.queue_add), tint = colors.textPrimary)
                }
            }
        }

        Reveal(1) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                SectionTitle(stringResource(Res.string.artist_popular))
                if (tracks.isEmpty()) {
                    Text(stringResource(Res.string.artist_no_tracks), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
                }
                Column {
                    tracks.forEachIndexed { index, track ->
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
        }

        if (loaded.albums.isNotEmpty()) {
            Reveal(2) {
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    SectionTitle(stringResource(Res.string.artist_albums))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                        items(loaded.albums, key = { it.id }) { album ->
                            AlbumCard(api, album, onClick = { onOpenAlbum(album.id, album.name) }, Modifier.width(148.dp), showArtists = false)
                        }
                    }
                }
            }
        }
    }
}
