package ru.starline.bluz

/**
 * Декодер спектра кадра `<B>`: отсчёты `uint32` по каналам, формат задаёт байт 96 заголовка:
 *  - 0 — отсчёты как есть, `uint32` little-endian;
 *  - 1 — DPCM + ZigZag + беззнаковый varint base-128 (младшая группа первой);
 *    разность считается по модулю 2^32, `prev = 0` в начале кадра.
 */
object SpectrumCodec {
    private const val MAX_VARINT_BYTES = 5

    /**
     * Декодирует [channels] каналов из `src[offset until limit]` в формате [format].
     *
     * @return отсчёты как `Double` (значение `uint32` без знака)
     * @throws IllegalArgumentException если формат неизвестен, поток обрезан или varint некорректен
     */
    fun decode(src: ByteArray, offset: Int, limit: Int, channels: Int, format: Int): DoubleArray {
        require(format == 0 || format == 1) { "unknown spectrum format $format" }
        return if (format == 0) readRaw(src, offset, limit, channels) else readPacked(src, offset, limit, channels)
    }

    private fun readRaw(src: ByteArray, offset: Int, limit: Int, channels: Int): DoubleArray {
        require(offset + 4 * channels <= limit) { "truncated raw spectrum" }
        return DoubleArray(channels) { i ->
            val base = offset + i * 4
            val value = (src[base].toInt() and 0xFF) or
                    ((src[base + 1].toInt() and 0xFF) shl 8) or
                    ((src[base + 2].toInt() and 0xFF) shl 16) or
                    ((src[base + 3].toInt() and 0xFF) shl 24)
            Integer.toUnsignedLong(value).toDouble()
        }
    }

    private fun readPacked(src: ByteArray, offset: Int, limit: Int, channels: Int): DoubleArray {
        val spectrum = DoubleArray(channels)
        var pos = offset
        var prev = 0
        for (i in 0 until channels) {
            var value = 0L
            var n = 0
            var b: Int
            do {
                require(pos < limit) { "truncated stream at channel $i" }
                require(n < MAX_VARINT_BYTES) { "varint longer than $MAX_VARINT_BYTES bytes at channel $i" }
                b = src[pos++].toInt()
                value = value or ((b and 0x7F).toLong() shl (7 * n))
                n++
            } while ((b and 0x80) != 0)
            require(value <= 0xFFFFFFFFL) { "varint exceeds 32 bits at channel $i" }
            val zigzag = value.toInt()
            prev += (zigzag ushr 1) xor -(zigzag and 1)
            spectrum[i] = Integer.toUnsignedLong(prev).toDouble()
        }
        return spectrum
    }
}
