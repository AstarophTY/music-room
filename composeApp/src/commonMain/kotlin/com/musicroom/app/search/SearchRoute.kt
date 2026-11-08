package com.musicroom.app.search

import androidx.compose.runtime.saveable.listSaver

/** Pages opened from the search results. */
sealed interface SearchRoute {
    data object Results : SearchRoute
    data class Mood(val index: Int) : SearchRoute
    data class Artist(val id: String, val name: String) : SearchRoute
    data class Album(val id: String, val name: String) : SearchRoute

    companion object {
        val stackSaver = listSaver<List<SearchRoute>, String>(
            save = { encode(it) },
            restore = { decode(it) },
        )

        internal fun encode(routes: List<SearchRoute>): List<String> = routes.flatMap { route ->
            when (route) {
                Results -> listOf("results", "", "")
                is Mood -> listOf("mood", route.index.toString(), "")
                is Artist -> listOf("artist", route.id, route.name)
                is Album -> listOf("album", route.id, route.name)
            }
        }

        internal fun decode(values: List<String>): List<SearchRoute> {
            // Restore stacks saved by older app versions, where fields shared a single string.
            val entries = if (values.firstOrNull()?.contains('|') == true) {
                values.map { it.split('|', limit = 3) }
            } else {
                values.chunked(3)
            }
            return entries.mapNotNull { fields ->
                val kind = fields.getOrNull(0)
                val id = fields.getOrNull(1) ?: return@mapNotNull null
                val name = fields.getOrNull(2).orEmpty()
                when (kind) {
                    "results" -> Results
                    "mood" -> id.toIntOrNull()?.let(::Mood)
                    "artist" -> Artist(id, name)
                    "album" -> Album(id, name)
                    else -> null
                }
            }
        }
    }
}
