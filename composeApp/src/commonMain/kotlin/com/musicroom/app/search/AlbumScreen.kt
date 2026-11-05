package com.musicroom.app.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
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
import com.composables.icons.lucide.Play
import com.musicroom.app.common.RemoteImage
import com.musicroom.app.common.Reveal
import com.musicroom.app.common.SectionTitle
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.album_songs
import com.musicroom.app.generated.resources.album_track_count
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.common_retry
import com.musicroom.app.generated.resources.playlists_play
import com.musicroom.app.generated.resources.queue_add
import com.musicroom.app.music.TrackMoreButton
import com.musicroom.app.music.TrackRow
import com.musicroom.app.music.albumLabel
import com.musicroom.app.player.PlayerController
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.AlbumDetailResponse
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * An album: cover, name, its artists (tap one to open them, [onOpenArtist]) and "Album · 2001 · 12 songs",
 * Play (the album as a whole) and Add to queue, then its songs in order.
 */
@Composable
fun AlbumScreen(
    api: MusicRoomApi,
    albumId: String,
    title: String,
    player: PlayerController,
    onBack: () -> Unit,
    onOpenArtist: (String, String) -> Unit,
) {
    val colors = AppTheme.colors
    var detail by remember(albumId) { mutableStateOf<AlbumDetailResponse?>(null) }
    var error by remember(albumId) { mutableStateOf<UiText?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(albumId, attempt) {
        error = null
        try {
            detail = api.album(albumId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
        }
    }

    SubScreenScaffold(title = detail?.album?.name ?: title, onBack = onBack) {
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
        val album = loaded.album
        val tracks = loaded.tracks

        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            RemoteImage(api, album.imageUrl, Modifier.size(132.dp).clip(AppShapes.large))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                Text(album.name, style = AppTheme.typography.title, color = colors.textPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    album.artists.forEach { artist ->
                        Text(
                            artist.name,
                            style = AppTheme.typography.label,
                            color = colors.accentText,
                            modifier = Modifier.pressable(onClick = { onOpenArtist(artist.id, artist.name) }),
                        )
                    }
                }
                Text(
                    listOfNotNull(
                        albumLabel(album),
                        album.trackCount?.let { pluralStringResource(Res.plurals.album_track_count, it, it) },
                    ).joinToString(" · "),
                    style = AppTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            AppButton(stringResource(Res.string.playlists_play), { player.playAlbum(album, tracks, 0) }, enabled = tracks.isNotEmpty(), icon = Lucide.Play)
            IconButton(onClick = { player.addToQueue(tracks) }, enabled = tracks.isNotEmpty()) {
                Icon(Lucide.ListPlus, contentDescription = stringResource(Res.string.queue_add), tint = colors.textPrimary)
            }
        }

        Reveal(1) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                SectionTitle(stringResource(Res.string.album_songs))
                Column {
                    tracks.forEachIndexed { index, track ->
                        TrackRow(
                            api,
                            track,
                            playing = player.track?.id == track.id,
                            onClick = { player.playAlbum(album, tracks, index) },
                            trailing = { TrackMoreButton(api, player, track) },
                            onSwipeRight = { player.addToQueue(listOf(track)) },
                        )
                    }
                }
            }
        }
    }
}
