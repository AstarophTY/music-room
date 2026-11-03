package com.musicroom.app.theme.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object AppShapes {
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    internal val material = Shapes(extraSmall = small, small = small, medium = medium, large = medium, extraLarge = medium)
}
