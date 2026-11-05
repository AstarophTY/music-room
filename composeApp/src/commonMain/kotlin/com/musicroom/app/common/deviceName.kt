package com.musicroom.app.common

import androidx.compose.runtime.Composable

/** What the app runs on, in the user's language ("phone", "computer", "browser"), for texts like "follows your phone". */
@Composable
expect fun deviceName(): String
