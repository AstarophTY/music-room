package com.musicroom.app.player

/**
 * What a focused event gives to the player's buttons. While one is set ([PlayerController.eventRemote]), play / pause
 * and next drive the event's music (its owner only) instead of the user's own, and the other buttons are greyed out.
 * Implemented by the event focus.
 */
interface EventRemote {
    /** The user may start, pause and skip the event's music (its owner), and no command is running. */
    val canControl: Boolean

    /** The event's music is playing (its status, whatever the local device does). */
    val playing: Boolean

    /** A song plays or is paused: there is something to skip. */
    val canSkip: Boolean

    fun togglePlay()

    fun skip()
}
