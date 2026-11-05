package com.musicroom.server.modules.premium

/**
 * Reads MP3 files (MPEG audio layer III): [durationMs] walks the frames one by one, so variable bitrate files get
 * their real length. Anything else (another format, a renamed file) gives null.
 */
object Mp3Audio {
    /** Frames in a row needed to call a file MP3. */
    private const val MIN_FRAMES = 10

    /** How far after the tags the first frame is looked for. */
    private const val MAX_SYNC_SEARCH = 64 * 1024

    private val BITRATES_V1 = intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320)
    private val BITRATES_V2 = intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160)
    private val SAMPLE_RATES = mapOf(
        3 to intArrayOf(44100, 48000, 32000),
        2 to intArrayOf(22050, 24000, 16000),
        0 to intArrayOf(11025, 12000, 8000),
    )

    /** Length of the MP3 file [bytes] in milliseconds, or null if it is not one. */
    fun durationMs(bytes: ByteArray): Long? {
        val start = afterId3(bytes)
        val limit = minOf(bytes.size, start + MAX_SYNC_SEARCH)
        var first = start
        while (first < limit) {
            val frame = frameAt(bytes, first)
            if (frame != null && frameAt(bytes, first + frame.length) != null) break
            first++
        }
        if (first >= limit) return null
        var position = first
        var frames = 0
        var samplesMs = 0.0
        while (true) {
            val frame = frameAt(bytes, position) ?: break
            frames++
            samplesMs += frame.samples * 1000.0 / frame.sampleRate
            position += frame.length
        }
        return if (frames >= MIN_FRAMES) samplesMs.toLong() else null
    }

    /** Where the audio starts, after an ID3v2 tag if there is one. */
    private fun afterId3(bytes: ByteArray): Int {
        if (bytes.size < 10 || bytes[0] != 'I'.code.toByte() || bytes[1] != 'D'.code.toByte() || bytes[2] != '3'.code.toByte()) return 0
        val size = (0 until 4).fold(0) { acc, index -> (acc shl 7) or (bytes[6 + index].toInt() and 0x7F) }
        val footer = if (bytes[5].toInt() and 0x10 != 0) 10 else 0
        return 10 + size + footer
    }

    private fun frameAt(bytes: ByteArray, offset: Int): Frame? {
        if (offset < 0 || offset + 4 > bytes.size) return null
        val b1 = bytes[offset + 1].toInt() and 0xFF
        val b2 = bytes[offset + 2].toInt() and 0xFF
        if (bytes[offset].toInt() and 0xFF != 0xFF || b1 and 0xE0 != 0xE0) return null
        val version = (b1 shr 3) and 3
        val layer = (b1 shr 1) and 3
        val bitrateIndex = (b2 shr 4) and 0xF
        val rateIndex = (b2 shr 2) and 3
        if (version == 1 || layer != 1 || bitrateIndex == 0 || bitrateIndex == 15 || rateIndex == 3) return null
        val mpeg1 = version == 3
        val bitrate = (if (mpeg1) BITRATES_V1 else BITRATES_V2)[bitrateIndex] * 1000
        val sampleRate = SAMPLE_RATES.getValue(version)[rateIndex]
        val padding = (b2 shr 1) and 1
        val length = (if (mpeg1) 144 else 72) * bitrate / sampleRate + padding
        if (offset + length > bytes.size) return null
        return Frame(length, if (mpeg1) 1152 else 576, sampleRate)
    }

    private class Frame(val length: Int, val samples: Int, val sampleRate: Int)
}
