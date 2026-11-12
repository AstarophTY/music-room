package com.musicroom.app.music

/** An Apple Music failure coming from the device itself, which the server knows nothing about. */
class AppleMusicException(message: String) : RuntimeException(message)
