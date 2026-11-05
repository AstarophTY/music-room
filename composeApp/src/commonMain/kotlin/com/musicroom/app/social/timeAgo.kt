package com.musicroom.app.social

import androidx.compose.runtime.Composable
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.time_days
import com.musicroom.app.generated.resources.time_hours
import com.musicroom.app.generated.resources.time_minutes
import com.musicroom.app.generated.resources.time_now
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource

/** How long ago [epochMs] was, in short: "now", "5 min", "3 h", "2 d". */
@Composable
fun timeAgo(epochMs: Long): String {
    val minutes = (Clock.System.now().toEpochMilliseconds() - epochMs).coerceAtLeast(0) / 60_000
    return when {
        minutes < 1 -> stringResource(Res.string.time_now)
        minutes < 60 -> stringResource(Res.string.time_minutes, minutes.toInt())
        minutes < 24 * 60 -> stringResource(Res.string.time_hours, (minutes / 60).toInt())
        else -> stringResource(Res.string.time_days, (minutes / (24 * 60)).toInt())
    }
}
