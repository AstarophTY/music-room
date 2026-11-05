package com.musicroom.app.events

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.LocateFixed
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Radio
import com.composables.icons.lucide.Users
import com.composables.icons.lucide.Vote
import com.musicroom.app.common.UiText
import com.musicroom.app.common.rememberLocationProvider
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.event_area_radius
import com.musicroom.app.generated.resources.event_area_set
import com.musicroom.app.generated.resources.event_area_unavailable
import com.musicroom.app.generated.resources.event_area_use_position
import com.musicroom.app.generated.resources.event_description_label
import com.musicroom.app.generated.resources.event_hours
import com.musicroom.app.generated.resources.event_hours_for
import com.musicroom.app.generated.resources.event_hours_keep
import com.musicroom.app.generated.resources.event_hours_none
import com.musicroom.app.generated.resources.event_license
import com.musicroom.app.generated.resources.event_license_everyone
import com.musicroom.app.generated.resources.event_license_everyone_body
import com.musicroom.app.generated.resources.event_license_invited
import com.musicroom.app.generated.resources.event_license_invited_body
import com.musicroom.app.generated.resources.event_license_location
import com.musicroom.app.generated.resources.event_license_location_body
import com.musicroom.app.generated.resources.event_location_failed
import com.musicroom.app.generated.resources.event_name_label
import com.musicroom.app.generated.resources.event_visibility
import com.musicroom.app.generated.resources.event_visibility_private
import com.musicroom.app.generated.resources.event_visibility_private_body
import com.musicroom.app.generated.resources.event_visibility_public
import com.musicroom.app.generated.resources.event_visibility_public_body
import com.musicroom.app.theme.components.AppButton
import com.musicroom.app.theme.components.AppButtonVariant
import com.musicroom.app.theme.components.AppChip
import com.musicroom.app.theme.components.AppTextField
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.EventValidation
import com.musicroom.shared.dto.CreateEventRequest
import com.musicroom.shared.dto.EventVisibility
import com.musicroom.shared.dto.EventVoteLicense
import com.musicroom.shared.dto.GeoPoint
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Settings of an event: name, description, who sees it and who votes. "In the area" uses the user's current
 * position, a radius, and how long votes stay open (from now). [initial] fills the form to edit an event.
 * [onSubmit] gets the checked request; its errors show under the form.
 */
@Composable
fun EventForm(
    initial: CreateEventRequest?,
    submitLabel: String,
    onSubmit: suspend (CreateEventRequest) -> Unit,
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val locate = rememberLocationProvider()
    // Radius choices in meters, and how long votes stay open in hours (null: no limit).
    val radii = listOf(100, 250, 500, 1_000, 5_000)
    val durations = listOf<Int?>(null, 1, 3, 6, 12, 24)
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var visibility by remember { mutableStateOf(initial?.visibility ?: EventVisibility.PUBLIC) }
    var license by remember { mutableStateOf(initial?.voteLicense ?: EventVoteLicense.EVERYONE) }
    var location by remember { mutableStateOf(initial?.location) }
    var radius by remember { mutableStateOf(initial?.radiusMeters ?: 250) }
    // An edited event keeps its hours until another choice is made.
    var keepHours by remember { mutableStateOf(initial?.endsAt != null) }
    var hours by remember { mutableStateOf<Int?>(3) }
    var locating by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }

    fun locateMe() {
        val provider = locate ?: return
        locating = true
        error = null
        scope.launch {
            val found: GeoPoint? = try {
                provider()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (found == null) error = UiText.of(Res.string.event_location_failed) else location = found
            locating = false
        }
    }

    fun submit() {
        val located = license == EventVoteLicense.LOCATION
        val now = Clock.System.now().toEpochMilliseconds()
        val request = CreateEventRequest(
            name = name.trim(),
            description = description.trim(),
            visibility = visibility,
            voteLicense = license,
            location = if (located) location else null,
            radiusMeters = if (located) radius else null,
            startsAt = when {
                !located -> null
                keepHours -> initial?.startsAt
                else -> hours?.let { now }
            },
            endsAt = when {
                !located -> null
                keepHours -> initial?.endsAt
                else -> hours?.let { now + it * 3_600_000L }
            },
        )
        EventValidation.error(request)?.let {
            error = UiText.of(it)
            return
        }
        busy = true
        error = null
        scope.launch {
            try {
                onSubmit(request)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = UiText.of(e)
            }
            busy = false
        }
    }

    val label: @Composable (String) -> Unit = { Text(it, style = AppTheme.typography.label, color = colors.textSecondary) }

    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
        AppTextField(name, { name = it }, stringResource(Res.string.event_name_label), Modifier.fillMaxWidth(), enabled = !busy, leadingIcon = Lucide.Radio)
        AppTextField(description, { description = it }, stringResource(Res.string.event_description_label), Modifier.fillMaxWidth(), enabled = !busy)

        label(stringResource(Res.string.event_visibility))
        ChoiceOption(
            Lucide.Globe,
            stringResource(Res.string.event_visibility_public),
            stringResource(Res.string.event_visibility_public_body),
            selected = visibility == EventVisibility.PUBLIC,
            onClick = { visibility = EventVisibility.PUBLIC },
            enabled = !busy,
        )
        ChoiceOption(
            Lucide.Lock,
            stringResource(Res.string.event_visibility_private),
            stringResource(Res.string.event_visibility_private_body),
            selected = visibility == EventVisibility.PRIVATE,
            onClick = { visibility = EventVisibility.PRIVATE },
            enabled = !busy,
        )

        label(stringResource(Res.string.event_license))
        ChoiceOption(
            Lucide.Vote,
            stringResource(Res.string.event_license_everyone),
            stringResource(Res.string.event_license_everyone_body),
            selected = license == EventVoteLicense.EVERYONE,
            onClick = { license = EventVoteLicense.EVERYONE },
            enabled = !busy,
        )
        ChoiceOption(
            Lucide.Users,
            stringResource(Res.string.event_license_invited),
            stringResource(Res.string.event_license_invited_body),
            selected = license == EventVoteLicense.INVITED,
            onClick = { license = EventVoteLicense.INVITED },
            enabled = !busy,
        )
        ChoiceOption(
            Lucide.MapPin,
            stringResource(Res.string.event_license_location),
            stringResource(Res.string.event_license_location_body),
            selected = license == EventVoteLicense.LOCATION,
            onClick = { license = EventVoteLicense.LOCATION },
            enabled = !busy,
        )
        AnimatedVisibility(license == EventVoteLicense.LOCATION) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                if (locate == null && location == null) {
                    Text(stringResource(Res.string.event_area_unavailable), style = AppTheme.typography.bodySmall, color = colors.error)
                } else {
                    AppButton(
                        stringResource(if (location == null) Res.string.event_area_use_position else Res.string.event_area_set),
                        ::locateMe,
                        Modifier.fillMaxWidth(),
                        enabled = !busy && !locating && locate != null,
                        variant = if (location == null) AppButtonVariant.Primary else AppButtonVariant.Secondary,
                        icon = Lucide.LocateFixed,
                    )
                }
                label(stringResource(Res.string.event_area_radius))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    radii.forEach { meters ->
                        AppChip(if (meters >= 1_000) "${meters / 1_000} km" else "$meters m", selected = radius == meters, onClick = { radius = meters })
                    }
                }
                label(stringResource(Res.string.event_hours))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    val end = initial?.endsAt
                    if (end != null) {
                        AppChip(stringResource(Res.string.event_hours_keep, clockTime(end)), selected = keepHours, onClick = { keepHours = true })
                    }
                    durations.forEach { option ->
                        AppChip(
                            if (option == null) stringResource(Res.string.event_hours_none) else stringResource(Res.string.event_hours_for, option),
                            selected = !keepHours && hours == option,
                            onClick = {
                                keepHours = false
                                hours = option
                            },
                        )
                    }
                }
            }
        }

        error?.let { Text(it.resolve(), style = AppTheme.typography.bodySmall, color = colors.error) }
        AppButton(submitLabel, ::submit, Modifier.fillMaxWidth(), enabled = !busy && !locating)
    }
}
