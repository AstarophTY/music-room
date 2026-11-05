package com.musicroom.app.common

/** An audio file chosen by the user: its content and its name (without the extension), to suggest a title. */
class PickedAudio(val bytes: ByteArray, val name: String)
