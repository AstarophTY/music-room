package com.musicroom.app.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.musicroom.app.theme.color.AppColorScheme

/**
 * Paints the map with the colours of the app instead of its own.
 *
 * The tiles come from OpenStreetMap in its green, blue and pink, which has nothing to do with the rest of
 * the screen. Each pixel is read for how light it is and painted somewhere between two colours of the
 * theme: the paper the map is drawn on and the ink of its streets. In the dark theme the two swap, so the
 * land goes dark and the streets light, instead of a white page under a dark screen.
 */
object MapTint {
    /** Weights of the eye, so that a yellow road and a blue river keep the brightness they had. */
    private const val RED = 0.299f
    private const val GREEN = 0.587f
    private const val BLUE = 0.114f

    /**
     * The map is nearly all pale: land, parks and water sit between [FLOOR] and white, and painting that
     * range over the whole ink-to-paper line is what keeps a street apart from the field beside it.
     * Values below [FLOOR] continue past the ink until the output channels are clamped.
     */
    private const val FLOOR = 0.55f

    fun of(colors: AppColorScheme, dark: Boolean): ColorFilter {
        val ink = if (dark) colors.textSecondary else colors.textPrimary
        val paper = if (dark) colors.surface else colors.background
        return ColorFilter.colorMatrix(duotone(shadow = ink, highlight = paper))
    }

    /** Sends [FLOOR] to [shadow] and white to [highlight], everything else along the line between them. */
    private fun duotone(shadow: Color, highlight: Color): ColorMatrix {
        val stretch = 1f / (1f - FLOOR)
        val red = (highlight.red - shadow.red) * stretch
        val green = (highlight.green - shadow.green) * stretch
        val blue = (highlight.blue - shadow.blue) * stretch
        // What the stretch takes off the bottom is given back in the offset, which is read in 0..255.
        val origin = { channel: Float, slope: Float -> (channel - slope * FLOOR) * 255f }
        return ColorMatrix(
            floatArrayOf(
                RED * red, GREEN * red, BLUE * red, 0f, origin(shadow.red, red),
                RED * green, GREEN * green, BLUE * green, 0f, origin(shadow.green, green),
                RED * blue, GREEN * blue, BLUE * blue, 0f, origin(shadow.blue, blue),
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }
}
