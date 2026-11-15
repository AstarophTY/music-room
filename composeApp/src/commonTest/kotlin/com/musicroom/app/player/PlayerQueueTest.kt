package com.musicroom.app.player

import com.musicroom.shared.dto.TrackResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerQueueTest {
    private fun track(id: String) = TrackResponse(id, id, emptyList(), "", durationMs = 1000, uri = id)

    @Test
    fun sentEntryCannotBeMovedRemovedOrUndoneUntilPlaybackConfirmsIt() = runTest {
        val queue = PlayerQueue(this)
        val added = queue.append(listOf(track("a") to null, track("b") to null, track("c") to null))
        queue.announce(added)
        val notice = requireNotNull(queue.notice)
        queue.markSent(added.first())
        queue.move(added.first().key, 2)
        queue.remove(added.first().key)
        queue.move(added.last().key, -2)
        assertEquals(listOf("a", "c", "b"), queue.entries.map { it.track.id })
        queue.undo(notice)
        assertEquals(listOf("a"), queue.entries.map { it.track.id })
        assertFalse(queue.canUndo(notice))
        queue.onTrackChanged("a")
        assertTrue(queue.entries.isEmpty())
        assertNull(queue.sentKey)
    }

    @Test
    fun newerNoticeReplacesPreviousTimeout() = runTest {
        val queue = PlayerQueue(this)
        val first = queue.append(listOf(track("a") to null))
        queue.announce(first)
        runCurrent()
        advanceTimeBy(3000)
        val second = queue.append(listOf(track("b") to null))
        queue.announce(second)
        runCurrent()
        advanceTimeBy(1000)
        assertEquals(second, queue.notice?.entries)
        advanceTimeBy(3000)
        runCurrent()
        assertNull(queue.notice)
    }
}
