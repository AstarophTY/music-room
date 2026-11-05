package com.musicroom.app.events

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Ticket
import com.musicroom.app.common.EmptyState
import com.musicroom.app.common.TabContent
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.events_create
import com.musicroom.app.generated.resources.events_empty_body
import com.musicroom.app.generated.resources.events_empty_title
import com.musicroom.app.generated.resources.events_title
import com.musicroom.app.theme.components.AppChip
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Find events: nearby, public, or the ones you were invited to. */
@Composable
fun EventsScreen(onCreate: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf(EventFilter.Nearby) }
    TabContent {
        Text(stringResource(Res.string.events_title), style = AppTheme.typography.display, color = AppTheme.colors.textPrimary)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            EventFilter.entries.forEach { option ->
                AppChip(stringResource(option.label), selected = option == filter, onClick = { filter = option })
            }
        }
        EmptyState(
            icon = Lucide.Ticket,
            title = stringResource(Res.string.events_empty_title),
            body = stringResource(Res.string.events_empty_body),
            actionLabel = stringResource(Res.string.events_create),
            onAction = onCreate,
        )
    }
}
