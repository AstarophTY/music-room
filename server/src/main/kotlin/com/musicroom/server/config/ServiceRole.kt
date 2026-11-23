package com.musicroom.server.config

/** The part of the API this process serves. [ALL] is the single server used by Docker Compose and the tests. */
enum class ServiceRole {
    ALL,
    IDENTITY,
    MUSIC,
    LIBRARY,
    REALTIME;

    /** Holds the WebSockets and plays the events' music. */
    val runsRealtime: Boolean get() = this == ALL || this == REALTIME

    /** Its social events must reach the WebSockets held by the realtime service. */
    val relaysSocialEvents: Boolean get() = !runsRealtime

    companion object {
        fun parse(value: String): ServiceRole =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown SERVICE_ROLE: $value")
    }
}
