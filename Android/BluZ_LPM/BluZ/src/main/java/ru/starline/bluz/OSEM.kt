import kotlin.math.*

class FastMLEM(private val nChannels: Int) {
    private val E_MIN = 0.02
    private val E_MAX = 4.0
    private val deltaE = (E_MAX - E_MIN) / nChannels
    private val energyCenters = DoubleArray(nChannels) {
        E_MIN + (it + 0.5) * deltaE
    }

    // Для каждой истинной энергии i: [j_start, j_end] — диапазон ненулевого отклика
    private val responseRanges = precomputeResponseRanges()

    // === SNIP (лёгкая версия) ===
    private fun snipBackground(spectrum: DoubleArray, iterations: Int = 15): DoubleArray {
        val n = spectrum.size
        val b = DoubleArray(n) { ln(ln(spectrum[it] + 1.0) + 1.0) }
        val temp = DoubleArray(n)

        for (k in 1..iterations) {
            val w = (1.5 * sqrt(k.toDouble())).toInt()
            for (i in 0 until n) {
                val left = maxOf(0, i - w)
                val right = minOf(n - 1, i + w)
                temp[i] = minOf(b[i], (b[left] + b[right]) * 0.5)
            }
            System.arraycopy(temp, 0, b, 0, n)
        }

        return DoubleArray(n) {
            val v = exp(exp(b[it]) - 1.0) - 1.0
            if (v > 0) v else 0.0
        }
    }

    // === MLEM без матрицы ===
    fun unfoldSpectrum(measured: DoubleArray, iterations: Int = 12): DoubleArray {
        require(measured.size == nChannels) { "Неверный размер" }

        val background = snipBackground(measured)
        val cleaned = DoubleArray(nChannels) { maxOf(0.0, measured[it] - background[it]) }

        var S = DoubleArray(nChannels) { 1.0 }

        repeat(iterations) {
            // Шаг 1: вычислить проекцию R * S
            val RS = DoubleArray(nChannels)
            for (i in 0 until nChannels) {
                val (jStart, jEnd) = responseRanges[i]
                val E_true = energyCenters[i]
                for (j in jStart until jEnd) {
                    val rVal = detectorResponseSingle(E_true, energyCenters[j])
                    RS[j] += rVal * S[i]
                }
            }

            // Шаг 2: вычислить коррекцию R^T * (M / RS)
            val correction = DoubleArray(nChannels)
            for (i in 0 until nChannels) {
                val (jStart, jEnd) = responseRanges[i]
                val E_true = energyCenters[i]
                var sum = 0.0
                for (j in jStart until jEnd) {
                    val ratio = if (RS[j] > 1e-10) cleaned[j] / RS[j] else 0.0
                    val rVal = detectorResponseSingle(E_true, energyCenters[j])
                    sum += rVal * ratio
                }
                correction[i] = sum
            }

            // Шаг 3: обновить S
            for (i in 0 until nChannels) {
                S[i] *= correction[i]
            }
        }
        return S
    }

    // === Предвычисление диапазонов отклика ===
    private fun precomputeResponseRanges(): Array<Pair<Int, Int>> {
        return Array(nChannels) { i ->
            val E_true = energyCenters[i]
            if (E_true <= 0.01) return@Array Pair(0, 0)

            val sigma = 0.07 * sqrt(0.662 / E_true) * E_true / 2.355
            val peakWidth = 4 * sigma

            // Пик: [E_true - peakWidth, E_true + peakWidth]
            val jPeakMin = ((E_true - peakWidth - E_MIN) / deltaE).toInt().coerceAtLeast(0)
            val jPeakMax = ((E_true + peakWidth - E_MIN) / deltaE).toInt().coerceAtMost(nChannels)

            // Комптон: до E_compton_max
            val E_compton_max = E_true / (1.0 + 0.255 / E_true)
            val jComptonMax = ((E_compton_max - E_MIN) / deltaE).toInt().coerceAtMost(nChannels)

            val jStart = 0
            val jEnd = maxOf(jPeakMax, jComptonMax).coerceAtMost(nChannels)

            Pair(jStart, jEnd)
        }
    }
    // === Вычисление одного элемента отклика ===
    private fun detectorResponseSingle(E_true: Double, E_det: Double): Double {
        if (E_true <= 0.01 || E_det <= 0) return 0.0

        val peakFrac = smoothPeakFraction(E_true)
        val sigma = 0.07 * sqrt(0.662 / E_true) * E_true / 2.355
        val peak = exp(-0.5 * ((E_det - E_true) / maxOf(sigma, 1e-6)).pow(2))

        val E_compton_max = E_true / (1.0 + 0.255 / E_true)
        val compton = if (E_det < E_compton_max && E_det < E_true) {
            1.0 - (E_compton_max - E_det) / (E_compton_max + 1e-6)
        } else 0.0

        val backscatter = if (abs(E_det - 0.22) < 0.1) {
            exp(-0.5 * ((E_det - 0.22) / 0.02).pow(2))
        } else 0.0

        return peakFrac * peak + (1.0 - peakFrac) * (0.9 * compton + 0.1 * backscatter)
    }

    private fun smoothPeakFraction(E: Double): Double {
        val E_knots = doubleArrayOf(0.02, 0.05, 0.15, 0.5, 1.5, 3.0)
        val eff_knots = doubleArrayOf(0.95, 0.80, 0.60, 0.35, 0.15, 0.08)
        if (E <= E_knots.first()) return eff_knots.first()
        if (E >= E_knots.last()) return eff_knots.last()
        for (i in 1 until E_knots.size) {
            if (E <= E_knots[i]) {
                val t = (E - E_knots[i - 1]) / (E_knots[i] - E_knots[i - 1])
                return eff_knots[i - 1] + t * (eff_knots[i] - eff_knots[i - 1])
            }
        }
        return eff_knots.last()
    }
}