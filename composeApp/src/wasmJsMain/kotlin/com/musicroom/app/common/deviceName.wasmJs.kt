package com.musicroom.app.common

import androidx.compose.runtime.Composable
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.device_browser
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun deviceName(): String = stringResource(Res.string.device_browser)
