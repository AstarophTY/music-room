package com.musicroom.app.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.composables.icons.lucide.CreditCard
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Sparkles
import com.musicroom.app.common.SubScreenScaffold
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.premium_active
import com.musicroom.app.generated.resources.premium_ends
import com.musicroom.app.generated.resources.premium_inactive
import com.musicroom.app.generated.resources.premium_manage
import com.musicroom.app.generated.resources.premium_pitch
import com.musicroom.app.generated.resources.premium_refresh
import com.musicroom.app.generated.resources.premium_renews
import com.musicroom.app.generated.resources.premium_subscribe
import com.musicroom.app.generated.resources.premium_title
import com.musicroom.app.generated.resources.premium_unavailable
import com.musicroom.app.generated.resources.premium_waiting
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppCard
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.SubscriptionResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

/**
 * Music Room Premium: what it gives, and its state. Subscribing and managing the subscription open Stripe's pages
 * in the browser; after opening the payment page, the state is read again every few seconds until Premium turns on.
 */
@Composable
fun PremiumScreen(api: MusicRoomApi, onBack: () -> Unit) {
    val colors = AppTheme.colors
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<SubscriptionResponse?>(null) }
    var error by remember { mutableStateOf<UiText?>(null) }
    var busy by remember { mutableStateOf(false) }
    // Bumped to read the state again; waiting: the payment page is open.
    var reload by remember { mutableIntStateOf(0) }
    var waiting by remember { mutableStateOf(false) }
    // While the payment page is open, the state is read every 3 s for about three minutes.
    val waitIntervalMs = 3_000L
    val waitChecks = 60

    LaunchedEffect(reload, waiting) {
        var checks = 0
        while (true) {
            try {
                val fetched = api.subscription()
                status = fetched
                if (fetched.premium) waiting = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
            }
            if (!waiting || ++checks > waitChecks) break
            delay(waitIntervalMs)
        }
    }

    /** Opens the Stripe page given by [page]. */
    fun open(page: suspend () -> String, thenWait: Boolean) {
        error = null
        busy = true
        scope.launch {
            try {
                uriHandler.openUri(page())
                if (thenWait) waiting = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
            }
            busy = false
        }
    }

    SubScreenScaffold(title = stringResource(Res.string.premium_title), onBack = onBack) {
        Text(stringResource(Res.string.premium_pitch), style = AppTheme.typography.body, color = colors.textSecondary)
        val shown = status
        when {
            shown == null -> Text(stringResource(Res.string.common_loading), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            !shown.configured -> Text(stringResource(Res.string.premium_unavailable), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            else -> {
                val renewsAt = shown.renewsAt
                val date = renewsAt?.let {
                    val day = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
                    "${day.day.toString().padStart(2, '0')}/${day.month.ordinal.plus(1).toString().padStart(2, '0')}/${day.year}"
                }
                AppCard(Modifier.fillMaxWidth()) {
                    SettingsRow(
                        Lucide.Sparkles,
                        stringResource(Res.string.premium_title),
                        onClick = {},
                        value = stringResource(if (shown.premium) Res.string.premium_active else Res.string.premium_inactive),
                        showChevron = false,
                    )
                }
                if (shown.premium && date != null) {
                    Text(
                        stringResource(if (shown.cancelAtPeriodEnd) Res.string.premium_ends else Res.string.premium_renews, date),
                        style = AppTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    if (shown.premium) {
                        AppButton(
                            stringResource(Res.string.premium_manage),
                            { open({ api.billingPortal().url }, thenWait = false) },
                            Modifier.fillMaxWidth(),
                            enabled = !busy,
                            variant = AppButtonVariant.Secondary,
                            icon = Lucide.CreditCard,
                        )
                    } else {
                        AppButton(
                            stringResource(Res.string.premium_subscribe),
                            { open({ api.premiumCheckout().url }, thenWait = true) },
                            Modifier.fillMaxWidth(),
                            enabled = !busy,
                            icon = Lucide.Sparkles,
                        )
                    }
                    AppButton(
                        stringResource(Res.string.premium_refresh),
                        { reload++ },
                        Modifier.fillMaxWidth(),
                        variant = AppButtonVariant.Ghost,
                        icon = Lucide.RefreshCw,
                    )
                }
                if (waiting) Text(stringResource(Res.string.premium_waiting), style = AppTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
    }
}
