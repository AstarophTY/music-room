package com.musicroom.app.theme.typography

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.bricolage_grotesque_regular
import com.musicroom.app.generated.resources.bricolage_grotesque_medium
import com.musicroom.app.generated.resources.bricolage_grotesque_semibold
import com.musicroom.app.generated.resources.bricolage_grotesque_bold

@Composable
fun bricolageFontFamily() = FontFamily(
    Font(Res.font.bricolage_grotesque_regular, FontWeight.Normal),
    Font(Res.font.bricolage_grotesque_medium, FontWeight.Medium),
    Font(Res.font.bricolage_grotesque_semibold, FontWeight.SemiBold),
    Font(Res.font.bricolage_grotesque_bold, FontWeight.Bold),
)

