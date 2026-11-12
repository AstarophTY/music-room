package com.musicroom.app.events

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.List
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Map
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.events_view_list
import com.musicroom.app.generated.resources.events_view_map
import com.musicroom.app.theme.components.AppChip
import com.musicroom.app.theme.tokens.AppTheme
import org.jetbrains.compose.resources.stringResource

/** Which events are listed, and whether they are shown on the map or one under the other. */
@Composable
fun EventFilterBar(
    filter: EventFilter,
    onFilter: (EventFilter) -> Unit,
    onMap: Boolean,
    onToggleView: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            EventFilter.entries.forEach { option ->
                AppChip(stringResource(option.label), selected = option == filter, onClick = { onFilter(option) })
            }
        }
        IconButton(onToggleView, Modifier.size(40.dp)) {
            Icon(
                if (onMap) Lucide.List else Lucide.Map,
                contentDescription = stringResource(if (onMap) Res.string.events_view_list else Res.string.events_view_map),
                tint = AppTheme.colors.textPrimary,
            )
        }
    }
}
