package com.musicroom.app.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.musicroom.app.common.StatusMessage
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.common.UiText
import com.musicroom.app.common.icon
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_please_wait
import com.musicroom.app.generated.resources.common_save
import com.musicroom.app.generated.resources.social_intro
import com.musicroom.app.generated.resources.social_invalid
import com.musicroom.app.generated.resources.social_saved
import com.musicroom.app.generated.resources.social_title
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.SocialLinks
import com.musicroom.shared.dto.SocialLink
import com.musicroom.shared.dto.SocialNetwork
import com.musicroom.shared.dto.UpdateSocialLinksRequest
import com.musicroom.shared.dto.UserResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** One field per network; an empty field removes it. Links and "@name" are cleaned before saving. */
@Composable
fun SocialLinksScreen(api: MusicRoomApi, user: UserResponse, onUserChange: (UserResponse) -> Unit, onBack: () -> Unit) {
    val handles = remember(user.socialLinks) {
        mutableStateMapOf<SocialNetwork, String>().apply { user.socialLinks.forEach { put(it.network, it.handle) } }
    }
    var status by remember { mutableStateOf<StatusMessage?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors

    fun save() {
        val links = SocialNetwork.entries.mapNotNull { network ->
            handles[network]?.let { SocialLink(network, SocialLinks.normalize(network, it)) }?.takeIf { it.handle.isNotEmpty() }
        }
        val invalid = links.firstOrNull { SocialLinks.handleError(it.network, it.handle) != null }
        if (invalid != null) {
            status = StatusMessage(UiText.of(Res.string.social_invalid, invalid.network.displayName), isError = true)
            return
        }
        busy = true
        status = null
        scope.launch {
            try {
                onUserChange(api.updateSocialLinks(UpdateSocialLinksRequest(links)))
                status = StatusMessage(UiText.of(Res.string.social_saved), isError = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status = StatusMessage(UiText.of(e), isError = true)
            } finally {
                busy = false
            }
        }
    }

    SubScreenScaffold(title = stringResource(Res.string.social_title), onBack = onBack) {
        Text(stringResource(Res.string.social_intro), style = AppTheme.typography.body, color = colors.textSecondary)
        SocialNetwork.entries.forEach { network ->
            AppTextField(
                value = handles[network].orEmpty(),
                onValueChange = { handles[network] = it },
                label = network.displayName,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                leadingIcon = network.icon(),
            )
        }
        status?.let { Text(it.text.resolve(), style = AppTheme.typography.bodySmall, color = if (it.isError) colors.error else colors.success) }
        AppButton(
            text = stringResource(if (busy) Res.string.common_please_wait else Res.string.common_save),
            onClick = { save() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
        )
    }
}
