package com.musicroom.app.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Users
import com.musicroom.app.common.StatusMessage
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.common_please_wait
import com.musicroom.app.generated.resources.common_save
import com.musicroom.app.generated.resources.error_profile_details_invalid
import com.musicroom.app.generated.resources.profile_details_friends
import com.musicroom.app.generated.resources.profile_details_intro
import com.musicroom.app.generated.resources.profile_details_music
import com.musicroom.app.generated.resources.profile_details_private
import com.musicroom.app.generated.resources.profile_details_public
import com.musicroom.app.generated.resources.profile_details_title
import com.musicroom.app.generated.resources.profile_saved
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.ProfileValidation
import com.musicroom.shared.dto.UpdateUserDetailsRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** What the user tells about themselves, by audience, and their music preferences (separated by commas). */
@Composable
fun ProfileDetailsScreen(api: MusicRoomApi, onBack: () -> Unit) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var publicInfo by remember { mutableStateOf("") }
    var friendsInfo by remember { mutableStateOf("") }
    var privateInfo by remember { mutableStateOf("") }
    var preferences by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<StatusMessage?>(null) }

    LaunchedEffect(Unit) {
        try {
            val details = api.userDetails()
            publicInfo = details.publicInfo
            friendsInfo = details.friendsInfo
            privateInfo = details.privateInfo
            preferences = details.musicPreferences.joinToString(", ")
            loaded = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            status = StatusMessage(UiText.of(e), isError = true)
        }
    }

    fun save() {
        val cleaned = ProfileValidation.cleanPreferences(preferences.split(','))
        if (!ProfileValidation.isValid(listOf(publicInfo, friendsInfo, privateInfo), cleaned)) {
            status = StatusMessage(UiText.of(Res.string.error_profile_details_invalid), isError = true)
            return
        }
        busy = true
        status = null
        scope.launch {
            try {
                val saved = api.updateUserDetails(UpdateUserDetailsRequest(publicInfo, friendsInfo, privateInfo, cleaned))
                preferences = saved.musicPreferences.joinToString(", ")
                status = StatusMessage(UiText.of(Res.string.profile_saved), isError = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status = StatusMessage(UiText.of(e), isError = true)
            } finally {
                busy = false
            }
        }
    }

    SubScreenScaffold(title = stringResource(Res.string.profile_details_title), onBack = onBack) {
        Text(stringResource(Res.string.profile_details_intro), style = AppTheme.typography.body, color = colors.textSecondary)
        if (!loaded && status == null) {
            Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
        }
        if (loaded) {
            AppTextField(publicInfo, { publicInfo = it }, stringResource(Res.string.profile_details_public), Modifier.fillMaxWidth(), enabled = !busy, leadingIcon = Lucide.Globe)
            AppTextField(friendsInfo, { friendsInfo = it }, stringResource(Res.string.profile_details_friends), Modifier.fillMaxWidth(), enabled = !busy, leadingIcon = Lucide.Users)
            AppTextField(privateInfo, { privateInfo = it }, stringResource(Res.string.profile_details_private), Modifier.fillMaxWidth(), enabled = !busy, leadingIcon = Lucide.Lock)
            AppTextField(preferences, { preferences = it }, stringResource(Res.string.profile_details_music), Modifier.fillMaxWidth(), enabled = !busy, leadingIcon = Lucide.Music)
        }
        status?.let { Text(it.text.resolve(), style = AppTheme.typography.bodySmall, color = if (it.isError) colors.error else colors.success) }
        if (loaded) {
            AppButton(
                text = stringResource(if (busy) Res.string.common_please_wait else Res.string.common_save),
                onClick = { save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
            )
        }
    }
}
