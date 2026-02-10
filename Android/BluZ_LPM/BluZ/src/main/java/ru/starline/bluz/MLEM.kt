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
// === ALS (Asymmetric Least Squares) для оценки фона ===

    private fun alsBackground(
        spectrum: DoubleArray,
        lambda: Double = 1e6,
        p: Double = 0.005,
        maxIter: Int = 10
    ): DoubleArray {
        require(spectrum.size == nChannels) { "Неверный размер спектра" }

        var z = spectrum.clone()

        repeat(maxIter) {
            // Вычисляем веса
            val weights = DoubleArray(nChannels) { i ->
                if (spectrum[i] >= z[i]) p else (1.0 - p)
            }

            // Правая часть: W * y
            val b = DoubleArray(nChannels) { weights[it] * spectrum[it] }

            // Решаем систему (W + λDᵀD)z = W*y
            z = solveALSConjugateGradient(weights, b, lambda)
        }

        return z.map { maxOf(0.0, it) }.toDoubleArray()
    }

    private fun solveALSConjugateGradient(
        weights: DoubleArray,
        b: DoubleArray,
        lambda: Double,
        cgMaxIter: Int = 50,
        cgTol: Double = 1e-6
    ): DoubleArray {
        val x = DoubleArray(nChannels) { 0.0 }
        var r = b - applyALSMatVec(weights, x, lambda)
        var pVec = r.clone()
        var rsOld = dot(r, r)

        for (i in 0 until cgMaxIter) {
            val Ap = applyALSMatVec(weights, pVec, lambda)
            val alpha = rsOld / dot(pVec, Ap)
            for (i in x.indices) {
                x[i] += pVec[i] * alpha
            }
            r -= Ap * alpha
            val rsNew = dot(r, r)
            if (sqrt(rsNew) < cgTol) break
            pVec = r + pVec * (rsNew / rsOld)
            rsOld = rsNew
        }
        return x
    }

    private fun applyALSMatVec(
        weights: DoubleArray,
        x: DoubleArray,
        lambda: Double
    ): DoubleArray {
        val result = DoubleArray(nChannels)
        // W * x
        for (i in 0 until nChannels) {
            result[i] = weights[i] * x[i]
        }
        // + λ * DᵀD * x (2-я производная)
        for (i in 2 until nChannels - 2) {
            result[i] += lambda * (
                    x[i - 2] - 4 * x[i - 1] + 6 * x[i] - 4 * x[i + 1] + x[i + 2]
                    )
        }
        // Граничные условия
        if (nChannels > 4) {
            result[0] += lambda * (6 * x[0] - 4 * x[1] + x[2])
            result[1] += lambda * (-4 * x[0] + 6 * x[1] - 4 * x[2] + x[3])
            result[nChannels - 2] += lambda * (x[nChannels - 4] - 4 * x[nChannels - 3] + 6 * x[nChannels - 2] - 4 * x[nChannels - 1])
            result[nChannels - 1] += lambda * (x[nChannels - 3] - 4 * x[nChannels - 2] + 6 * x[nChannels - 1])
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

    private fun medianFilter(spectrum: DoubleArray, windowSize: Int): DoubleArray {
        require(windowSize % 2 == 1) { "Window size must be odd" }
        val radius = windowSize / 2
        return DoubleArray(spectrum.size) { i ->
            val window = DoubleArray(windowSize) { j ->
                spectrum[(i + j - radius).coerceIn(0, spectrum.size - 1)]
            }
            window.sortedArray()[windowSize / 2]
        }
    }


    /* MLEM деконволюция */
    suspend fun ufldSpectrum(measured: DoubleArray, iterations: Int = 25, onProgress: suspend (Int) -> Unit = {}): DoubleArray {
        require(measured.size == nChannels) { "Размер спектра != $nChannels" }
        val denoisedMeasured = medianFilter(measured, 21)
        // Вычитание фона
        val background = snipBackground(measured)

        //val background = alsBackground(denoisedMeasured, lambda = 1e6, p = 0.005)

        val cleaned = DoubleArray(nChannels) { maxOf(0.0, denoisedMeasured[it] - background[it]) }
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

    /* TV-MAP-MLEM деконволюция */
    suspend fun ufldSpectrumTV(
        measured: DoubleArray,
        iterations: Int = 30,
        beta: Double = 0.1,
        medianWindowSize: Int = 5,
        onProgress: suspend (Int) -> Unit = {}
    ): DoubleArray {
        require(measured.size == nChannels) { "Размер спектра != $nChannels" }

        // 1. Предобработка с минимальной фильтрацией
        // Используем небольшое окно, чтобы сохранить детали
        val denoised = medianFilter(measured, medianWindowSize)

        // 2. Оценка фона на слегка сглаженных данных
        // SNIP с умеренными параметрами
        val background = snipBackground(denoised, iterations = 15, wFactor = 1.5)

        // 3. Очищенный спектр (фон вычитается, но без отрицательных значений)
        val cleaned = DoubleArray(nChannels) { i ->
            maxOf(0.0, denoised[i] - background[i])
        }

        // 4. Инициализация через улучшенный backprojection
        var S = initializeSpectrumBackprojection(cleaned)

        onProgress(1)

        // 5. Предвычисление R^T * 1 (суммы по столбцам матрицы отклика)
        // Это экономит время на каждой итерации
        val RtOnes = DoubleArray(nChannels) { i ->
            var sum = 0.0
            for (j in 0 until nChannels) {
                sum += responseMatrix[j][i]
            }
            sum
        }

        // 6. Итеративный TV-MAP-MLEM алгоритм
        for (iter in 0 until iterations) {
            // 6.1. Проекция: R * S (моделируем измерение)
            val RS = DoubleArray(nChannels) { j ->
                var sum = 0.0
                for (i in 0 until nChannels) {
                    sum += responseMatrix[j][i] * S[i]
                }
                sum
            }

            // 6.2. Вычисление отношения измеренного к модельным данным
            // Защита от деления на ноль и отрицательных значений
            val ratio = DoubleArray(nChannels) { j ->
                val rsj = RS[j]
                if (rsj > 1e-12 && cleaned[j] > 0) {
                    cleaned[j] / rsj
                } else {
                    0.0
                }
            }

            // 6.3. Backprojection: R^T * (M / (R*S))
            val backprojection = DoubleArray(nChannels) { i ->
                var sum = 0.0
                for (j in 0 until nChannels) {
                    sum += responseMatrix[j][i] * ratio[j]
                }
                sum
            }

            // 6.4. Вычисление TV-градиента (Total Variation)
            val tvGrad = computeTVGradient(S)

            // 6.5. Обновление спектра с TV-регуляризацией
            S = DoubleArray(nChannels) { i ->
                // Базовый множитель MLEM
                val mlemFactor = backprojection[i]

                // TV регуляризация добавляется в знаменатель
                // β - сила регуляризации (0 = чистая MLEM)
                val tvTerm = beta * tvGrad[i]
                val denominator = RtOnes[i] + tvTerm

                // Обновление с защитой от отрицательных знаменателей
                if (denominator > 1e-12) {
                    S[i] * mlemFactor / denominator
                } else {
                    // Если знаменатель отрицательный или нулевой,
                    // используем обычное MLEM обновление без TV
                    S[i] * mlemFactor / max(RtOnes[i], 1e-12)
                }
            }

            // 6.6. Небольшое сглаживание каждые 5 итераций для стабильности
            if (iter % 5 == 0 && iter > 0) {
                S = mildSmoothing(S)
            }

            // 6.7. Гарантируем неотрицательность
            S = S.map { maxOf(1e-12, it) }.toDoubleArray()

            // 6.8. Прогресс (1-100%)
            val progress = ((iter + 1) * 100 / iterations).coerceIn(1, 100)
            onProgress(progress + 2)
        }

        return S
    }

    /* TV-MAP-MLEM деконволюция */
    /*
    suspend fun ufldSpectrumTV(
        measured: DoubleArray,
        iterations: Int = 30,
        beta: Double = 0.1,
        medianWindowSize: Int = 5,
        onProgress: suspend (Int) -> Unit = {}
    ): DoubleArray {
        require(measured.size == nChannels) { "Размер спектра != $nChannels" }

        // 1. Предобработка с минимальной фильтрацией
        // Используем небольшое окно, чтобы сохранить детали
        val denoised = medianFilter(measured, medianWindowSize)

        // 2. Оценка фона на слегка сглаженных данных
        // SNIP с умеренными параметрами
        val background = snipBackground(denoised, iterations = 15, wFactor = 1.5)

        // 3. Очищенный спектр (фон вычитается, но без отрицательных значений)
        val cleaned = DoubleArray(nChannels) { i ->
            maxOf(0.0, denoised[i] - background[i])
        }

        // 4. Инициализация через улучшенный backprojection
        var S = initializeSpectrumBackprojection(cleaned)

        onProgress(1)

        // 5. Предвычисление R^T * 1 (суммы по столбцам матрицы отклика)
        // Это экономит время на каждой итерации
        val RtOnes = DoubleArray(nChannels) { i ->
            var sum = 0.0
            for (j in 0 until nChannels) {
                sum += responseMatrix[j][i]
            }
            sum
        }

        // 6. Итеративный TV-MAP-MLEM алгоритм
        for (iter in 0 until iterations) {
            // 6.1. Проекция: R * S (моделируем измерение)
            val RS = DoubleArray(nChannels) { j ->
                var sum = 0.0
                for (i in 0 until nChannels) {
                    sum += responseMatrix[j][i] * S[i]
                }
                sum
            }

            // 6.2. Вычисление отношения измеренного к модельным данным
            // Защита от деления на ноль и отрицательных значений
            val ratio = DoubleArray(nChannels) { j ->
                val rsj = RS[j]
                if (rsj > 1e-12 && cleaned[j] > 0) {
                    cleaned[j] / rsj
                } else {
                    0.0
                }
            }

            // 6.3. Backprojection: R^T * (M / (R*S))
            val backprojection = DoubleArray(nChannels) { i ->
                var sum = 0.0
                for (j in 0 until nChannels) {
                    sum += responseMatrix[j][i] * ratio[j]
                }
                sum
            }

            // 6.4. Вычисление TV-градиента (Total Variation)
            val tvGrad = computeTVGradient(S)

            // 6.5. Обновление спектра с TV-регуляризацией
            S = DoubleArray(nChannels) { i ->
                // Базовый множитель MLEM
                val mlemFactor = backprojection[i]

                // TV регуляризация добавляется в знаменатель
                // β - сила регуляризации (0 = чистая MLEM)
                val tvTerm = beta * tvGrad[i]
                val denominator = RtOnes[i] + tvTerm

                // Обновление с защитой от отрицательных знаменателей
                if (denominator > 1e-12) {
                    S[i] * mlemFactor / denominator
                } else {
                    // Если знаменатель отрицательный или нулевой,
                    // используем обычное MLEM обновление без TV
                    S[i] * mlemFactor / max(RtOnes[i], 1e-12)
                }
            }

            // 6.6. Небольшое сглаживание каждые 5 итераций для стабильности
            if (iter % 5 == 0 && iter > 0) {
                S = mildSmoothing(S)
            }

            // 6.7. Гарантируем неотрицательность
            S = S.map { maxOf(1e-12, it) }.toDoubleArray()

            // 6.8. Прогресс (1-100%)
            val progress = ((iter + 1) * 100 / iterations).coerceIn(1, 100)
            onProgress(progress)
        }

        return S
    }*/



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

    /* TV-MAP-MLEM + median + snip */
    /*
    suspend fun ufldSpectrumTV(
        measured: DoubleArray,
        iterations: Int = 15,
        beta: Double = 0.05,
        medianWindowSize: Int = 7,
        onProgress: suspend (Int) -> Unit = {}
    ): DoubleArray {
        require(measured.size == nChannels)

        // 1. Предобработка
        val denoised = medianFilter(measured, medianWindowSize)
        val background = snipBackground(denoised, iterations = 20)
        val cleaned = DoubleArray(nChannels) {
            maxOf(0.0, denoised[it] - background[it])
        }

        // 2. Инициализация (лучше использовать uniform или backprojection)
        var S = initializeSpectrum(cleaned)

        // 3. Предвычисление R^T * 1 (суммы по столбцам)
        val RtOnes = DoubleArray(nChannels) { i ->
            (0 until nChannels).sumOf { j -> responseMatrix[j][i] }
        }

        for (iter in 0 until iterations) {
            // 3.1. Проекция R*S
            val RS = DoubleArray(nChannels) { j ->
                (0 until nChannels).sumOf { i -> responseMatrix[j][i] * S[i] }
            }

            // 3.2. Вычисление отношения M/(R*S)
            val ratio = DoubleArray(nChannels) { j ->
                if (RS[j] > 1e-10) cleaned[j] / RS[j] else 0.0
            }

            // 3.3. Backprojection R^T * ratio
            val backprojection = DoubleArray(nChannels) { i ->
                (0 until nChannels).sumOf { j -> responseMatrix[j][i] * ratio[j] }
            }

            // 3.4. TV-градиент
            val tvGrad = computeTVGradient(S)

            // 3.5. Обновление MAP-MLEM с TV-регуляризацией
            S = DoubleArray(nChannels) { i ->
                val denominator = RtOnes[i] + beta * tvGrad[i]
                if (denominator > 1e-10) {
                    S[i] * backprojection[i] / denominator
                } else {
                    S[i] * backprojection[i]
                }
            }.map { maxOf(1e-10, it) }.toDoubleArray()

            // 3.6. Нормализация (опционально)
            //val sum = S.sum()
            //if (sum > 0) {
            //    S = S.map { it / sum }.toDoubleArray()
            //}

            onProgress(iter + 1)
        }
        return S
    }*/


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
/*
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
*/

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
                    (1 + x*x) / (E_true * (1 - x))
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