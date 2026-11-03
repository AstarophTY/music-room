package com.musicroom.app.theme.typography

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle


data class AppTypography(val display: TextStyle, val title: TextStyle, val heading: TextStyle, val body: TextStyle, val bodySmall: TextStyle, val label: TextStyle) {
    internal fun material() = Typography(
        displayLarge = display, displayMedium = display, displaySmall = display,
        headlineLarge = title, headlineMedium = title, headlineSmall = heading,
        titleLarge = title, titleMedium = heading, titleSmall = heading,
        bodyLarge = body, bodyMedium = body, bodySmall = bodySmall,
        labelLarge = label, labelMedium = label, labelSmall = label,
    )
}

