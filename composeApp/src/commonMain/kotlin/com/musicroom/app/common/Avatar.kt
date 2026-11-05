package com.musicroom.app.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.dto.AvatarColor

/** Round avatar: the photo if there is one, otherwise the initials on the chosen color. */
@Composable
fun Avatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: AvatarColor = AvatarColor.YELLOW,
    photo: ImageBitmap? = null,
) {
    val tint = AppTheme.colors.avatars.getValue(color)
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(tint.background, CircleShape)
            .border(AppTheme.spacing.border, tint.border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(photo, contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            val initials = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
                .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
            val fontSize = (size.value * 0.38f).sp
            // Line height equal to the font size, trimmed and centered, so the letters sit in the middle of the
            // circle (the label's own 20sp line height pushed them off-center on desktop).
            val style = TextStyle(
                fontSize = fontSize,
                lineHeight = fontSize,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            )
            Text(initials, style = AppTheme.typography.label.merge(style), color = tint.text, maxLines = 1)
        }
    }
}
