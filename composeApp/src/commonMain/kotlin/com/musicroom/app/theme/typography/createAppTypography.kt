package com.musicroom.app.theme.typography

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
internal fun createAppTypography(): AppTypography {
    val family = bricolageFontFamily()
    return AppTypography(
        TextStyle(fontFamily = family, fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
        TextStyle(fontFamily = family, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
        TextStyle(fontFamily = family, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
        TextStyle(fontFamily = family, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
        TextStyle(fontFamily = family, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
        TextStyle(fontFamily = family, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    )
}
