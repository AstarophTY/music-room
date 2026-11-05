package com.musicroom.app.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Upload
import com.musicroom.app.common.EmptyState
import com.musicroom.app.common.PickedAudio
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.common.UiText
import com.musicroom.app.common.rememberAudioPicker
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_cancel
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.my_songs_artist_field
import com.musicroom.app.generated.resources.my_songs_delete
import com.musicroom.app.generated.resources.my_songs_empty_body
import com.musicroom.app.generated.resources.my_songs_empty_title
import com.musicroom.app.generated.resources.my_songs_no_picker
import com.musicroom.app.generated.resources.my_songs_premium_needed
import com.musicroom.app.generated.resources.my_songs_see_premium
import com.musicroom.app.generated.resources.my_songs_send
import com.musicroom.app.generated.resources.my_songs_title
import com.musicroom.app.generated.resources.my_songs_title_field
import com.musicroom.app.generated.resources.my_songs_upload
import com.musicroom.app.generated.resources.my_songs_upload_hint
import com.musicroom.app.generated.resources.my_songs_uploading
import com.musicroom.app.music.TrackMoreButton
import com.musicroom.app.music.TrackRow
import com.musicroom.app.player.PlayerController
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.TrackResponse
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The songs the user uploaded to Music Room. With Premium, an MP3 file is chosen, given a title and an artist (the
 * user by default), then uploaded. Each song plays in the app, goes to the queue or a playlist, or is deleted.
 * Without Premium, the songs already uploaded stay and a card leads to [onOpenPremium].
 */
@Composable
fun MySongsScreen(api: MusicRoomApi, user: UserResponse, player: PlayerController, onOpenPremium: () -> Unit, onBack: () -> Unit) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var songs by remember { mutableStateOf<List<TrackResponse>?>(null) }
    var premium by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf<UiText?>(null) }
    var picked by remember { mutableStateOf<PickedAudio?>(null) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var uploading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            premium = api.subscription().premium
            songs = api.myHostedTracks().items
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
        }
    }
    val pickFile = rememberAudioPicker { file ->
        if (file != null) {
            picked = file
            title = file.name.take(100)
            artist = user.displayName
            error = null
        }
    }

    fun upload(file: PickedAudio) {
        uploading = true
        error = null
        scope.launch {
            try {
                val song = api.uploadHostedTrack(file.bytes, title.trim(), artist.trim())
                songs = listOf(song) + songs.orEmpty()
                picked = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
            }
            uploading = false
        }
    }

    fun delete(song: TrackResponse) {
        error = null
        scope.launch {
            try {
                api.deleteHostedTrack(song.id.removePrefix(TrackResponse.HOSTED_PREFIX))
                songs = songs?.filterNot { it.id == song.id }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
            }
        }
    }

    SubScreenScaffold(title = stringResource(Res.string.my_songs_title), onBack = onBack) {
        val file = picked
        when {
            premium == false -> AppCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    Text(stringResource(Res.string.my_songs_premium_needed), style = AppTheme.typography.body, color = colors.textPrimary)
                    AppButton(stringResource(Res.string.my_songs_see_premium), onOpenPremium, Modifier.fillMaxWidth(), icon = Lucide.Sparkles)
                }
            }
            premium == null -> Unit
            pickFile == null -> Text(stringResource(Res.string.my_songs_no_picker), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            file == null -> Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                AppButton(stringResource(Res.string.my_songs_upload), pickFile, Modifier.fillMaxWidth(), icon = Lucide.Upload)
                Text(stringResource(Res.string.my_songs_upload_hint), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            }
            else -> AppCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    AppTextField(title, { title = it.take(100) }, stringResource(Res.string.my_songs_title_field), Modifier.fillMaxWidth(), enabled = !uploading)
                    AppTextField(artist, { artist = it.take(100) }, stringResource(Res.string.my_songs_artist_field), Modifier.fillMaxWidth(), enabled = !uploading)
                    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                        AppButton(
                            stringResource(Res.string.common_cancel),
                            { picked = null },
                            Modifier.weight(1f),
                            enabled = !uploading,
                            variant = AppButtonVariant.Ghost,
                        )
                        AppButton(
                            stringResource(if (uploading) Res.string.my_songs_uploading else Res.string.my_songs_send),
                            { upload(file) },
                            Modifier.weight(1f),
                            enabled = !uploading && title.isNotBlank(),
                            icon = Lucide.Upload,
                        )
                    }
                }
            }
        }
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }

        val list = songs
        when {
            list == null -> if (error == null) {
                Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            }
            list.isEmpty() -> EmptyState(Lucide.Music, stringResource(Res.string.my_songs_empty_title), stringResource(Res.string.my_songs_empty_body))
            else -> Column {
                list.forEachIndexed { index, song ->
                    TrackRow(
                        api,
                        song,
                        playing = player.track?.id == song.id,
                        onClick = { player.play(list, index) },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TrackMoreButton(api, player, song)
                                IconButton(onClick = { delete(song) }) {
                                    Icon(Lucide.Trash2, contentDescription = stringResource(Res.string.my_songs_delete), tint = colors.textSecondary)
                                }
                            }
                        },
                        onSwipeRight = { player.addToQueue(listOf(song)) },
                    )
                }
            }
        }
    }
}
