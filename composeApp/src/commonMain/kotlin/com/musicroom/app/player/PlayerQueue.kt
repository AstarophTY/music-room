package com.musicroom.app.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.musicroom.shared.dto.TrackResponse
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Owns the app queue, its remote handoff and the undo notification. */
class PlayerQueue(private val scope: CoroutineScope) {
    var entries by mutableStateOf<List<QueueEntry>>(emptyList())
        private set
    var sentKey by mutableStateOf<Long?>(null)
        private set
    var notice by mutableStateOf<QueueNotice?>(null)
        private set
    private var nextKey = 0L
    private var nextNoticeId = 0L
    private var noticeJob: Job? = null

    fun append(songs: List<Pair<TrackResponse, Proposer?>>): List<QueueEntry> {
        val added = songs.map { (track, proposer) -> QueueEntry(nextKey++, track, proposer) }
        entries = entries + added
        return added
    }

    fun takeFirst(): QueueEntry? {
        val first = entries.firstOrNull() ?: return null
        entries = entries.drop(1)
        return first
    }

    fun remove(entry: QueueEntry) { entries = entries - entry }
    fun bringToFront(entry: QueueEntry) { entries = listOf(entry) + entries.filterNot { it.key == entry.key } }
    fun markSent(entry: QueueEntry) { sentKey = entry.key }
    fun cancelSent(key: Long) { if (sentKey == key) sentKey = null }
    fun onTrackChanged(id: String?) {
        val sent = entries.firstOrNull { it.key == sentKey }
        if (sent != null && sent.track.id == id) {
            remove(sent)
            sentKey = null
        }
    }

    fun announce(added: List<QueueEntry>) {
        if (added.isEmpty()) return
        noticeJob?.cancel()
        notice = QueueNotice(nextNoticeId++, added)
        noticeJob = scope.launch { delay(4.seconds); notice = null }
    }

    fun canUndo(value: QueueNotice): Boolean = value.entries.any { entry -> entry.key != sentKey && entries.any { it.key == entry.key } }
    fun undo(value: QueueNotice) {
        val keys = value.entries.map { it.key }.toSet() - setOfNotNull(sentKey)
        entries = entries.filterNot { it.key in keys }
        dismissNotice()
    }
    fun dismissNotice() { noticeJob?.cancel(); notice = null }

    fun move(key: Long, offset: Int) {
        val from = entries.indexOfFirst { it.key == key }
        if (from < 0 || key == sentKey) return
        val to = (from + offset).coerceIn(if (sentKey != null) 1 else 0, entries.lastIndex)
        if (to != from) entries = entries.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun remove(key: Long) { if (key != sentKey) entries = entries.filterNot { it.key == key } }
}
