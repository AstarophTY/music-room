package com.musicroom.app.player

/** Confirmation that [entries] were added to the queue. [id] changes with each new confirmation. */
data class QueueNotice(val id: Long, val entries: List<QueueEntry>)
