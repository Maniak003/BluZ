package ru.starline.bluz
import kotlin.math.*

class MLEM(private val nChannels: Int) {
    private val E_MIN = 0.02 // МэВ
    private val E_MAX = 2.8  // МэВ
    private val deltaE = (E_MAX - E_MIN) / nChannels
    private val energyCenters = DoubleArray(nChannels) {
        E_MIN + (it + 0.5) * deltaE
    }

    // Предвычисленная матрица отклика R[j][i] — j: измеренный канал, i: истинная энергия
    private val responseMatrix: Array<DoubleArray> = generateResponseMatrix()

    /* ALS  */
    private fun alsBackground(
        spectrum: DoubleArray,
        lambda: Double = 1e6,
        p: Double = 0.005
    ): DoubleArray {
        // Проверка на нулевой спектр
        if (spectrum.sum() < 1e-10) {
            return DoubleArray(nChannels) { 0.0 }
        }

        var z = spectrum.clone()
        repeat(10) {
            val weights = DoubleArray(nChannels) { i ->
                if (spectrum[i] >= z[i]) p else (1.0 - p)
            }
            val b = DoubleArray(nChannels) { weights[it] * spectrum[it] }
            z = solveALS(weights, b, lambda)

            // Защита от NaN/Inf
            for (i in z.indices) {
                if (z[i].isInfinite() || z[i].isNaN()) {
                    z[i] = 0.0
                }
            }
        }
        return z.map { maxOf(0.0, it) }.toDoubleArray()
    }

    private fun solveALS(weights: DoubleArray, b: DoubleArray, lambda: Double): DoubleArray {
        val x = DoubleArray(nChannels) { 0.0 }
        var r = DoubleArray(nChannels) { b[it] - applyALSMatVec(weights, x, lambda)[it] }
        var pVec = r.clone()
        var rsOld = dot(r, r)

        for (iter in 0 until 30){
            val Ap = applyALSMatVec(weights, pVec, lambda)
            val alpha = rsOld / dot(pVec, Ap)

            // Явное обновление (без операторов!)
            for (i in x.indices) {
                x[i] += alpha * pVec[i]
                r[i] -= alpha * Ap[i]
            }

            val rsNew = dot(r, r)
            if (rsNew < 1e-12) break

            val beta = rsNew / rsOld
            for (i in pVec.indices) {
                pVec[i] = r[i] + beta * pVec[i]
            }
            rsOld = rsNew
        }
        return x
    }

    private fun applyALSMatVec(weights: DoubleArray, x: DoubleArray, lambda: Double): DoubleArray {
        val result = DoubleArray(nChannels)
        for (i in result.indices) {
            result[i] = weights[i] * x[i]
        }
        // 2-я производная
        for (i in 2 until nChannels - 2) {
            result[i] += lambda * (x[i-2] - 4*x[i-1] + 6*x[i] - 4*x[i+1] + x[i+2])
        }
        return result
    }

    // === Вспомогательные операторы ===
    private operator fun DoubleArray.minus(other: DoubleArray): DoubleArray {
        return DoubleArray(size) { this[it] - other[it] }
    }

    private operator fun DoubleArray.plus(other: DoubleArray): DoubleArray {
        return DoubleArray(size) { this[it] + other[it] }
    }

    private operator fun DoubleArray.times(scalar: Double): DoubleArray {
        return DoubleArray(size) { this[it] * scalar }
    }

    private fun dot(a: DoubleArray, b: DoubleArray): Double {
        return a.indices.sumOf { a[it] * b[it] }
    }

    // SNIP: адаптивное вычитание фона
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

    // Медианный фильтр
    private fun medianFilter(spectrum: DoubleArray, windowSize: Int): DoubleArray {
        require(windowSize % 2 == 1)
        val radius = windowSize / 2
        return DoubleArray(spectrum.size) { i ->
            val window = DoubleArray(windowSize) { j ->
                spectrum[(i + j - radius).coerceIn(0, spectrum.size - 1)]
            }
            window.sortedArray()[windowSize / 2]
        }
    }


    /* MLEM деконволюция */
    suspend fun ufldSpectrum(
        measured: DoubleArray, // всегда 4096
        iterations: Int = 20,
        onProgress: suspend (Int) -> Unit = {}
    ): DoubleArray {
        require(measured.size >= nChannels) {
            "Размер спектра (${measured.size}) < nChannels ($nChannels)"
        }

        // Работаем только с первыми nChannels
        val workingSpectrum = DoubleArray(nChannels) { i ->
            measured[i]
        }

        // 1. Подавление импульсного шума
        val denoised = medianFilter(workingSpectrum, 5)

        // 2. Оценка фона через ALS
        val background = alsBackground(denoised, lambda = 1e6, p = 0.005)

        // 3. Чистый спектр
        val cleaned = DoubleArray(nChannels) {
            maxOf(0.0, denoised[it] - background[it])
        }

        // 4. Деконволюция
        var S = DoubleArray(nChannels) { i ->
            var sum = 0.0
            for (j in 0 until nChannels) {
                if (workingSpectrum[j] > 0) {
                    sum += responseMatrix[j][i] * workingSpectrum[j]
                }
            }
            maxOf(1.0, sum) // Минимальное значение 1.0
        }
        onProgress(1)

        repeat(iterations) {
            // R * S
            val RS = DoubleArray(nChannels) { j ->
                (0 until nChannels).sumOf { i -> responseMatrix[j][i] * S[i] }
            }
            // Коррекция
            val correction = DoubleArray(nChannels) { i ->
                (0 until nChannels).sumOf { j ->
                    val ratio = if (RS[j] > 1e-10) cleaned[j] / RS[j] else 0.0
                    responseMatrix[j][i] * ratio
                }
            }
            S = DoubleArray(nChannels) { S[it] * correction[it] }
            onProgress(it + 2)
        }

        // Возвращаем результат в исходном размере (4096)
        return DoubleArray(measured.size) { i ->
            if (i < nChannels) S[i] else 0.0
        }
    }
    /* TV-MAP-MLEM деконволюция */
    suspend fun ufldSpectrumTV(
        measured: DoubleArray, // всегда 4096
        iterations: Int = 15,
        beta: Double = 0.01,
        windowSize: Int = 15,
        onProgress: suspend (Int) -> Unit = {}
    ): DoubleArray {
        // Обрабатываем ТОЛЬКО первые nChannels
        require(measured.size >= nChannels) {
            "Размер спектра (${measured.size}) < nChannels ($nChannels)"
        }

        // Создаём рабочий массив нужного размера
        val workingSpectrum = DoubleArray(nChannels) { i ->
            measured[i] // Берём только первые nChannels
        }

        // 1. Подавление импульсного шума
        val denoised = medianFilter(workingSpectrum, windowSize)

        // 2. Оценка фона через ALS
        val background = alsBackground(denoised, lambda = 1e6, p = 0.005)

        // 3. Чистый спектр
        val cleaned = DoubleArray(nChannels) {
            maxOf(0.0, denoised[it] - background[it])
        }

        // Инициализация через обратную проекцию
        var S = DoubleArray(nChannels) { i ->
            var sum = 0.0
            for (j in 0 until nChannels) {
                if (workingSpectrum[j] > 0) {
                    sum += responseMatrix[j][i] * workingSpectrum[j]
                }
            }
            maxOf(1.0, sum) // Гарантируем минимальное значение
        }
        onProgress(1)

        // 5. Предвычисление R^T * 1
        val RtOnes = DoubleArray(nChannels) { i ->
            (0 until nChannels).sumOf { j -> responseMatrix[j][i] }
        }

        // 6. TV-MAP-MLEM итерации
        for (iter in 0 until iterations) {
            // Проекция
            val RS = DoubleArray(nChannels) { j ->
                (0 until nChannels).sumOf { i -> responseMatrix[j][i] * S[i] }
            }

            // Отношение
            val ratio = DoubleArray(nChannels) { j ->
                if (RS[j] > 1e-10) cleaned[j] / RS[j] else 0.0
            }

            // Backprojection
            val backprojection = DoubleArray(nChannels) { i ->
                (0 until nChannels).sumOf { j -> responseMatrix[j][i] * ratio[j] }
            }

            // TV-градиент
            val tvGrad = computeTVGradient(S)

            // Обновление с TV-регуляризацией
            S = DoubleArray(nChannels) { i ->
                val denominator = RtOnes[i] + beta * maxOf(0.0, tvGrad[i])
                if (denominator > 1e-10) {
                    S[i] * backprojection[i] / denominator
                } else {
                    S[i] * backprojection[i]
                }
            }.map { maxOf(1e-12, it) }.toDoubleArray()

            onProgress(iter + 2)
        }

        // Возвращаем результат в исходном размере (4096)
        return DoubleArray(measured.size) { i ->
            if (i < nChannels) S[i] else 0.0
        }
    }



    /* Улучшенная инициализация через backprojection */
    private fun initializeSpectrumBackprojection(cleaned: DoubleArray): DoubleArray {
        val n = cleaned.size

        // Если спектр почти пустой, равномерное распределение
        val totalCounts = cleaned.sum()
        if (totalCounts <= 1e-10) {
            return DoubleArray(n) { 1.0 / n }
        }

        // Backprojection инициализация
        val init = DoubleArray(n) { i ->
            var sum = 0.0
            for (j in 0 until n) {
                // Учитываем только значимые измерения
                if (cleaned[j] > 0 && responseMatrix[j][i] > 0) {
                    sum += responseMatrix[j][i] * cleaned[j]
                }
            }
            maxOf(1e-12, sum)
        }

        // Нормировка начального приближения
        val initSum = init.sum()
        return if (initSum > 0) {
            init.map { it / initSum }.toDoubleArray()
        } else {
            DoubleArray(n) { 1.0 / n }
        }
    }

    /* Легкое сглаживание для стабильности */
    private fun mildSmoothing(spectrum: DoubleArray): DoubleArray {
        val n = spectrum.size
        val smoothed = spectrum.copyOf()

        if (n < 3) return spectrum

        // Простое скользящее среднее с весами [0.25, 0.5, 0.25]
        for (i in 1 until n - 1) {
            smoothed[i] = 0.25 * spectrum[i-1] + 0.5 * spectrum[i] + 0.25 * spectrum[i+1]
        }

        return smoothed.map { maxOf(1e-12, it) }.toDoubleArray()
    }

    /* TV-градиент */
    private fun computeTVGradient(S: DoubleArray, epsilon: Double = 1e-6): DoubleArray {
        val grad = DoubleArray(S.size)

        for (i in 1 until S.size - 1) {
            val diffPrev = S[i] - S[i-1]
            val diffNext = S[i+1] - S[i]

            val term1 = diffPrev / sqrt(diffPrev * diffPrev + epsilon)
            val term2 = diffNext / sqrt(diffNext * diffNext + epsilon)

            grad[i] = term1 - term2
        }

        // Граничные условия
        if (S.size > 1) {
            val diffFirst = S[1] - S[0]
            grad[0] = -diffFirst / sqrt(diffFirst * diffFirst + epsilon)

            val diffLast = S[S.size-1] - S[S.size-2]
            grad[S.size-1] = diffLast / sqrt(diffLast * diffLast + epsilon)
        }

        return grad
    }

    // Улучшенная инициализация с учетом особенностей гамма-спектрометрии
    fun initializeSpectrum(cleaned: DoubleArray): DoubleArray {
        val n = cleaned.size

        // 1. Нормируем измеренный спектр
        val total = cleaned.sum()
        if (total <= 0) return DoubleArray(n) { 1.0 / n }

        val normalized = cleaned.map { it / total }.toDoubleArray()

        // 2. Применяем легкое сглаживание (медианный фильтр 3 точки)
        val smoothed = medianFilter(normalized, 3)

        // 3. Гарантируем минимальное значение
        return smoothed.map { maxOf(1e-8, it) }.toDoubleArray()
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
        require(E_true > 0) { "Энергия должна быть положительной" }

        val response = DoubleArray(nChannels)

        // 1. Фотопик (Гаусс)
        val fwhm = 0.07 * sqrt(0.662 / E_true) * E_true
        val sigma = fwhm / 2.355
        val peakFraction = smoothPeakFraction(E_true)

        for (j in 0 until nChannels) {
            val E_det = energyCenters[j]
            val diff = E_det - E_true

            // Гауссов пик
            val gaussian = if (sigma > 1e-6) {
                exp(-0.5 * (diff / sigma).pow(2)) / (sigma * sqrt((2 * PI).toDouble()))
            } else {
                if (abs(diff) < deltaE/2) 1.0 / deltaE else 0.0
            }

            // Комптоновское распределение (упрощенное)
            val compton = if (E_det > 0 && E_det < E_true) {
                val E_compton_max = E_true / (1.0 + 0.511 / (2 * E_true))
                if (E_det <= E_compton_max) {
                    // Klein-Nishina приближение (упрощенное)
                    val x = E_det / E_true
                    if (abs(1 - x) < 1e-6) {
                        0.0 // или малое значение
                    } else {
                        (1 + x*x) / (E_true * (1 - x))
                    }
                } else 0.0
            } else 0.0

            // Backscatter пик (~200 кэВ)
            val backscatter = exp(-0.5 * ((E_det - 0.2) / 0.03).pow(2))

            response[j] = peakFraction * gaussian +
                    (1 - peakFraction) * (0.85 * compton + 0.15 * backscatter)
        }

        // Нормировка на единицу
        val sum = response.sum()
        return if (sum > 0) response.map { it / sum }.toDoubleArray()
        else DoubleArray(nChannels) { 0.0 }
    }

    fun calculateExposureRate(
        unfoldedSpectrum: DoubleArray, // спектр после MLEM (фотоны за всё время)
        acquisitionTimeSeconds: Double  // время набора спектра в секундах
    ): Double { // возвращает МЭД в мкЗв/ч
        require(unfoldedSpectrum.size >= nChannels) { "Неверный размер спектра" }

        // Параметры энергетической сетки
        val E_MIN = this.E_MIN
        val E_MAX = this.E_MAX
        val deltaE = this.deltaE

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
        for (i in 0 until nChannels) { // ← Только до nChannels!
            val photonsTotal = unfoldedSpectrum[i]
            if (photonsTotal <= 0) continue

            val energy = E_MIN + (i + 0.5) * deltaE
            val muEnRho = interpolateMuEnRho(energy)
            val photonFlux = photonsTotal / acquisitionTimeSeconds
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