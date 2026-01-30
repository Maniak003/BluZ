package ru.starline.bluz
import kotlin.math.*

class MLEM(private val nChannels: Int) {
    private val E_MIN = 0.02 // МэВ
    private val E_MAX = 4.0  // МэВ
    private val deltaE = (E_MAX - E_MIN) / nChannels
    private val energyCenters = DoubleArray(nChannels) {
        E_MIN + (it + 0.5) * deltaE
    }

    // Предвычисленная матрица отклика R[j][i] — j: измеренный канал, i: истинная энергия
    private val responseMatrix: Array<DoubleArray> = generateResponseMatrix()

    // === SNIP: адаптивное вычитание фона ===
    private fun snipBackground(spectrum: DoubleArray, iterations: Int = 25, wFactor: Double = 2.0): DoubleArray {
        val n = spectrum.size
        val s = DoubleArray(n) { ln(ln(spectrum[it] + 1.0) + 1.0) }
        val b = s.clone()

        for (k in 1..iterations) {
           val w = (wFactor * sqrt(k.toDouble())).toInt()
            for (i in 0 until n) {
                val left = maxOf(0, i - w)
                val right = minOf(n - 1, i + w)
                b[i] = minOf(b[i], (b[left] + b[right]) / 2.0)
            }
        }

        return DoubleArray(n) { exp(exp(b[it]) - 1.0) - 1.0 }.map { maxOf(0.0, it) }.toDoubleArray()
    }

    /* MLEM деконволюция */
    suspend fun unfoldSpectrum(measured: DoubleArray, iterations: Int = 25, onProgress: suspend (Int) -> Unit = {}): DoubleArray {
        require(measured.size == nChannels) { "Размер спектра != $nChannels" }

        // Вычитание фона
        val background = snipBackground(measured)
        val cleaned = DoubleArray(nChannels) { maxOf(0.0, measured[it] - background[it]) }
        onProgress(1)
        // Инициализация
        var S = DoubleArray(nChannels) { 1.0 }
        for (iter in 0 until iterations) {
            // R * S
            val RS = DoubleArray(nChannels) { j ->
                var sum = 0.0
                for (i in 0 until nChannels) {
                    sum += responseMatrix[j][i] * S[i]
                }
                sum
            }
            // Коррекция: R^T * (M / (R*S))
            val correction = DoubleArray(nChannels) { i ->
                var sum = 0.0
                for (j in 0 until nChannels) {
                    val ratio = if (RS[j] > 1e-10) cleaned[j] / RS[j] else 0.0
                    sum += responseMatrix[j][i] * ratio
                }
                sum
            }

            // Обновление
            S = DoubleArray(nChannels) { S[it] * correction[it] }
            onProgress(iter + 2)
        }
        return S
    }

    /* Плавная эффективность */
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

    /* Создаем матрицу отклика */
    private fun generateResponseMatrix(): Array<DoubleArray> {
        val R = Array(nChannels) { DoubleArray(nChannels) }
        for (i in 0 until nChannels) {
            val resp = detectorResponse(energyCenters[i])
            val total = resp.sum()
            for (j in 0 until nChannels) {
                R[j][i] = if (total > 0) resp[j] / total else 0.0
            }
        }
        return R
    }

    private fun detectorResponse(E_true: Double): DoubleArray {
        if (E_true <= 0.01) return DoubleArray(nChannels) { 0.0 }

        val peakFrac = smoothPeakFraction(E_true)
        val sigma = 0.07 * sqrt(0.662 / E_true) * E_true / 2.355
        val peak = DoubleArray(nChannels) { j ->
            val diff = energyCenters[j] - E_true
            exp(-0.5 * (diff / maxOf(sigma, 1e-6)).pow(2))
        }

        val E_compton_max = E_true / (1.0 + 0.255 / E_true)
        val compton = DoubleArray(nChannels) { j ->
            val E_det = energyCenters[j]
            if (E_det > 0 && E_det < E_compton_max && E_det < E_true) {
                1.0 - (E_compton_max - E_det) / (E_compton_max + 1e-6)
            } else 0.0
        }

        val backscatter = DoubleArray(nChannels) { j ->
            val diff = energyCenters[j] - 0.22
            exp(-0.5 * (diff / 0.02).pow(2))
        }

        return DoubleArray(nChannels) { j ->
            peakFrac * peak[j] + (1.0 - peakFrac) * (0.9 * compton[j] + 0.1 * backscatter[j])
        }
    }

    fun calculateExposureRate(
        unfoldedSpectrum: DoubleArray, // спектр после MLEM (фотоны за всё время)
        acquisitionTimeSeconds: Double  // время набора спектра в секундах
    ): Double { // возвращает МЭД в мкЗв/ч
        require(unfoldedSpectrum.size == nChannels) { "Неверный размер спектра" }

        // Параметры энергетической сетки
        val E_MIN = 0.02 // МэВ
        val E_MAX = 4.0  // МэВ
        val deltaE = (E_MAX - E_MIN) / nChannels

        // Табличные данные (μ_en/ρ)_air из NIST XCOM
        val energyPoints = doubleArrayOf(
            0.01, 0.015, 0.02, 0.03, 0.04, 0.05, 0.06, 0.08, 0.1, 0.15, 0.2, 0.3,
            0.4, 0.5, 0.6, 0.8, 1.0, 1.25, 1.5, 2.0, 3.0, 4.0
        )
        val muEnRhoValues = doubleArrayOf(
            8.33, 5.33, 3.97, 2.27, 1.53, 1.14, 0.893, 0.598, 0.429, 0.249, 0.170, 0.109,
            0.0786, 0.0603, 0.0487, 0.0349, 0.0272, 0.0216, 0.0181, 0.0141, 0.0102, 0.0085
        )

        // Линейная интерполяция (можно заменить на более точную, но для скорости — линейная)
        fun interpolateMuEnRho(energy: Double): Double {
            if (energy <= energyPoints[0]) return muEnRhoValues[0]
            if (energy >= energyPoints.last()) return muEnRhoValues.last()

            for (i in 1 until energyPoints.size) {
                if (energy <= energyPoints[i]) {
                    val t = (energy - energyPoints[i - 1]) / (energyPoints[i] - energyPoints[i - 1])
                    return muEnRhoValues[i - 1] + t * (muEnRhoValues[i] - muEnRhoValues[i - 1])
                }
            }
            return muEnRhoValues.last()
        }

        var integral = 0.0
        for (i in 0 until nChannels) {
            val photonsTotal = unfoldedSpectrum[i]
            if (photonsTotal <= 0) continue

            val energy = E_MIN + (i + 0.5) * deltaE // центр канала, МэВ
            val muEnRho = interpolateMuEnRho(energy)

            // Поток фотонов в секунду
            val photonFlux = photonsTotal / acquisitionTimeSeconds

            // Вклад в интеграл: Φ(E) * E * (μ_en/ρ)
            integral += photonFlux * energy * muEnRho
        }

        // Коэффициент 0.869 переводит в Р/ч
        val exposureRate_RperH = 0.869 * integral * deltaE

        // Перевод в мкЗв/ч (1 Р ≈ 10 мЗв → 1 Р/ч = 10 000 мкЗв/ч)
        val exposureRate_uSvPerH = exposureRate_RperH * 10000.0
        val exposureRate_uRPerH = exposureRate_RperH * 1000000.0

        //return exposureRate_uSvPerH
        return exposureRate_uRPerH
    }

}