package com.musicroom.app.music

/** Track times as the player shows them: "3:07", or "1:02:03" past an hour. */
object Durations {
    fun format(ms: Long): String {
        val total = (ms.coerceAtLeast(0) / 1000).toInt()
        val hours = total / 3600
        val minutes = total % 3600 / 60
        val seconds = (total % 60).toString().padStart(2, '0')
        return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:$seconds" else "$minutes:$seconds"
    }
}
