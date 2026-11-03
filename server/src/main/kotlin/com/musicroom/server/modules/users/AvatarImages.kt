package com.musicroom.server.modules.users

import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO

/**
 * Turns an uploaded picture into a square 256x256 JPEG. Re-encoding drops metadata (GPS position...)
 * and anything that is not image data; the size is checked before decoding (no decompression bombs).
 */
object AvatarImages {
    const val MAX_UPLOAD_BYTES = 5 * 1024 * 1024
    private const val SIZE = 256
    private const val MAX_PIXELS = 40_000_000L

    /** Returns null if the bytes are not a supported image (JPEG, PNG, GIF, BMP). */
    fun toAvatarJpeg(bytes: ByteArray): ByteArray? = try {
        decode(bytes)?.let { encode(squareThumbnail(it)) }
    } catch (e: IOException) {
        null
    }

    private fun decode(bytes: ByteArray): BufferedImage? {
        ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { input ->
            val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: return null
            try {
                reader.input = input
                if (reader.getWidth(0).toLong() * reader.getHeight(0) > MAX_PIXELS) return null
                return reader.read(0)
            } finally {
                reader.dispose()
            }
        }
    }

    /** Center crop to a square, scaled to [SIZE]. Transparent pixels become white (JPEG has no alpha). */
    private fun squareThumbnail(source: BufferedImage): BufferedImage {
        val side = minOf(source.width, source.height)
        val x = (source.width - side) / 2
        val y = (source.height - side) / 2
        return BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB).also { target ->
            val g = target.createGraphics()
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                g.color = Color.WHITE
                g.fillRect(0, 0, SIZE, SIZE)
                g.drawImage(source, 0, 0, SIZE, SIZE, x, y, x + side, y + side, null)
            } finally {
                g.dispose()
            }
        }
    }

    private fun encode(image: BufferedImage): ByteArray =
        ByteArrayOutputStream().also { ImageIO.write(image, "jpg", it) }.toByteArray()
}
