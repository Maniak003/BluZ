package ru.starline.bluz

/**
 * Коррекция дифференциальной нелинейности 4096-канального АЦП, ширина каналов которого
 * повторяется каждые [PERIOD] каналов (алгоритм из AtomSpectra). Таблица ширин подбирается
 * по сырому накопленному спектру при каждом вызове; новые отсчёты делятся на текущую таблицу
 * и накапливаются, поэтому скорректированный спектр не убывает, пока прибор набирает данные.
 */
class DnlCorrector {
    private var lastRaw: DoubleArray? = null
    private var accumulated = DoubleArray(CHANNEL_COUNT)

    fun reset() {
        lastRaw = null
    }

    fun apply(raw: DoubleArray): DoubleArray {
        if (raw.size != CHANNEL_COUNT) {
            reset()
            return raw.clone()
        }
        val table = fit(raw)
        val last = lastRaw
        var anchor = last == null
        if (last != null) {
            for (i in raw.indices) {
                if (raw[i] < last[i]) {
                    anchor = true
                    break
                }
            }
        }
        if (anchor) accumulated = DoubleArray(CHANNEL_COUNT)
        val corrected = DoubleArray(CHANNEL_COUNT)
        for (i in raw.indices) {
            val added = if (anchor) raw[i] else raw[i] - last!![i]
            accumulated[i] += if (table == null) added else added / table[i % PERIOD]
            corrected[i] = Math.round(accumulated[i]).toDouble()
        }
        lastRaw = raw.clone()
        return corrected
    }

    /**
     * Относительные ширины каналов по channel % PERIOD, подтянутые к 1 тем сильнее, чем слабее
     * картина выделяется из счётного шума; null, пока не выделяется.
     */
    private fun fit(raw: DoubleArray): DoubleArray? {
        val table = DoubleArray(PERIOD) { 1.0 }
        val corrected = DoubleArray(raw.size)
        val counts = DoubleArray(PERIOD)
        val expected = DoubleArray(PERIOD)
        repeat(ITERATIONS) {
            for (i in raw.indices) corrected[i] = raw[i] / table[i % PERIOD]
            val baseline = baseline(corrected)
            counts.fill(0.0)
            expected.fill(0.0)
            for (i in FIRST_CHANNEL until LAST_CHANNEL) {
                if (baseline[i] <= MIN_BASELINE) continue
                counts[i % PERIOD] += raw[i]
                expected[i % PERIOD] += baseline[i]
            }
            val mean = counts.sum() / expected.sum()
            var chi2 = 0.0
            for (phase in 0 until PERIOD) {
                val deviation = counts[phase] - mean * expected[phase]
                chi2 += deviation * deviation / expected[phase]
            }
            if (!(chi2 >= MIN_CHI2)) return null
            val strength = 1 - (PERIOD - 1) / chi2
            for (phase in 0 until PERIOD) {
                table[phase] = 1 + strength * (counts[phase] / expected[phase] / mean - 1)
            }
        }
        return table
    }

    // значение в каждом канале квадратичной аппроксимации по соседям, сам канал не учитывается
    private fun baseline(values: DoubleArray): DoubleArray {
        var s0 = 0.0
        var s2 = 0.0
        var s4 = 0.0
        for (x in -HALF_WIDTH..HALF_WIDTH) {
            if (x == 0) continue
            s0++
            s2 += x * x
            s4 += x.toDouble() * x * x * x
        }
        val determinant = s0 * s4 - s2 * s2
        val last = values.size - 1
        return DoubleArray(values.size) { i ->
            var t0 = 0.0
            var t2 = 0.0
            for (x in -HALF_WIDTH..HALF_WIDTH) {
                if (x == 0) continue
                val value = values[minOf(last, maxOf(0, i + x))]
                t0 += value
                t2 += x * x * value
            }
            (s4 * t0 - s2 * t2) / determinant
        }
    }

    private companion object {
        const val CHANNEL_COUNT = 4096
        const val PERIOD = 64
        // полуширина локальной квадратичной аппроксимации, заменяющей спектр без DNL
        const val HALF_WIDTH = 12
        const val FIRST_CHANNEL = 100
        const val LAST_CHANNEL = 4000
        const val MIN_BASELINE = 2.0
        // свёрнутый хи-квадрат (PERIOD - 1 степеней свободы, шум Пуассона), ниже которого картину не отличить от шума
        const val MIN_CHI2 = 150.0
        const val ITERATIONS = 3
    }
}
