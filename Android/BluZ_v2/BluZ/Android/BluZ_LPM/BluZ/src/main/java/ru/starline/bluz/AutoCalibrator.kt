package ru.starline.bluz

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Автокалибровка энергетической шкалы спектрометра по источнику **Ra-226 в равновесии**
 * с дочерними изотопами (Pb-214, Bi-214). Содержит чистую расчётную логику без UI и без
 * сетевого I/O; всё взаимодействие с прибором (запись ВВ/компаратора/полинома) живёт в
 * [AutoCalibrationController].
 *
 * ## Физическое основание
 *
 * В равновесии Ra-226 даёт характерный набор хорошо разнесённых гамма-линий:
 *
 * | Энергия, кэВ | Изотоп  | Интенсивность | Роль                              |
 * |-------------:|:--------|:--------------|:----------------------------------|
 * | 295          | Pb-214  | ~19%          | вспомогательный                   |
 * | **352**      | Pb-214  | ~37%          | **низкоэнергетический якорь**     |
 * | **609**      | Bi-214  | ~46%          | **центральный якорь (ярчайший)**  |
 * | 1120         | Bi-214  | ~15%          | вспомогательный (часто слит с 1238)|
 * | **1764**     | Bi-214  | ~15%          | **высокоэнергетический якорь**    |
 *
 * Якорная тройка `352 / 609 / 1764` максимально разнесена по диапазону 0..3 МэВ, что
 * прибор покрывает целиком, и даёт устойчивое решение полинома 2-й степени
 * `E(ch) = A·ch² + B·ch + C`.
 *
 * ## Поведение при нештатных условиях
 *
 * Активность источника **не меняет** изотопный состав — пики всегда на тех же энергиях.
 * Но при слабом источнике их «топит» естественный фон (K-40 1460 кэВ, Tl-208 2614 кэВ,
 * радон-связанные линии из стройматериалов). Алгоритм возвращает один из ошибочных
 * вариантов [Result], а не пишет криво в прибор.
 *
 * ## Параметры детектора
 *
 * Прибор использует сцинтилляторы NaI(Tl) Ø10×40 мм или CsI(Tl) ~10×10×50 мм.
 * Энергетическое разрешение FWHM ≈ 7–9% при 662 кэВ; для 4096-канального режима на
 * 0–3500 кэВ это даёт ширину пика 609 кэВ ≈ 50 каналов. Окна поиска и центроиды
 * масштабируются по [fwhmChannels].
 *
 * ## Связанные файлы
 *
 * - [Mtrx] — решение системы 3×3 методом Крамера, используется для финального полинома.
 * - [AutoCalibrationController] — внешний оркестратор фаз (ВВ, компаратор, накопление).
 * - [globalObj.batteryPercent] / [globalObj.formatBattery] — пример той же дисциплины:
 *   чистая расчётная логика выносится в `globalObj`, UI остаётся в Activity/Fragment.
 */
object AutoCalibrator {

    /** Якорная тройка энергий Ra-226 (в кэВ), на которой строится квадратичная калибровка.
     *
     *  **Подбор для маленького детектора (NaI ⌀10×40 / CsI ~10×10×50).** При высоких
     *  энергиях эффективность маленького кристалла резко падает; пики 1120 и особенно 1764
     *  на бытовом источнике Ra-226 неразличимы в шуме. Реально видны только:
     *   - 82 кэВ — рентгеновская линия свинца (K-альфа из электронного перехода в Bi-214/Pb-214)
     *   - 352 кэВ — Pb-214 (гамма)
     *   - 609 кэВ — Bi-214 (гамма, ярчайший в семействе на этом детекторе)
     *
     *  Если 82 кэВ всё-таки скрыт компараторным фоном (бывает при слабом источнике или
     *  высоком пороге компаратора), алгоритм fallback'ится на пару 352/609 → линейная
     *  калибровка (см. [analyze]). */
    val anchorEnergiesKev: IntArray = intArrayOf(82, 352, 609)

    /** Якорная пара для линейной калибровки. **82+609 кэВ** — низкий и высокий якоря,
     *  дают максимальный рычаг по диапазону энергий. На маленьком детекторе оба пика
     *  видны почти всегда: 82 кэВ — ярчайший пик слева, 609 кэВ — самый правый значимый. */
    val fallbackAnchorPair: IntArray = intArrayOf(82, 609)

    /** Дополнительные ожидаемые пики Ra-226 в низкоэнергетической области — используются
     *  для верификации найденной тройки: если в спектре по этим энергиям тоже есть
     *  пик-кандидат, score решения растёт. Высокоэнергетические линии (1120/1238/1764)
     *  убраны — они неразличимы на маленьком сцинтилляторе. */
    val auxEnergiesKev: IntArray = intArrayOf(186, 242, 295)

    /** Целевая верхняя граница диапазона спектра, кэВ. Используется для расчёта целевого
     *  канала пика 1764 в [AutoCalibrationController]. См. также `targetChannelFor(...)`. */
    const val TARGET_FULL_RANGE_KEV: Int = 3500

    /** Минимальная сумма счётов в окне ±FWHM вокруг каждого якорного пика, ниже которой
     *  идентификация считается ненадёжной → [Result.WeakSignal].
     *  500 — компромисс: достаточно для надёжного центроида, но не вынуждает копить
     *  слишком долго при слабом источнике. */
    const val MIN_COUNTS_PER_PEAK: Int = 500

    /**
     * Целевой канал для пика энергии [energyKev] при заданном [channels] разрешении спектра,
     * исходя из того, что верхняя кромка спектра отвечает [TARGET_FULL_RANGE_KEV].
     */
    fun targetChannelFor(energyKev: Int, channels: Int): Int =
        (channels.toLong() * energyKev / TARGET_FULL_RANGE_KEV).toInt().coerceIn(1, channels - 1)

    // ───────────────────────────── Sealed Result ─────────────────────────────

    /** Результат единичной попытки автокалибровки. */
    sealed class Result {
        /** Калибровка выполнена. Содержит готовый полином и подробности найденных пиков. */
        data class Ok(
            val cA: Float,
            val cB: Float,
            val cC: Float,
            val peaks: List<IdentifiedPeak>,
            /** RMS-невязка предсказанных vs ожидаемых энергий в кэВ. <2 кэВ — отлично. */
            val residualKev: Float,
        ) : Result()

        /** Один или несколько якорных пиков не набрали [MIN_COUNTS_PER_PEAK] счётов. */
        object WeakSignal : Result()

        /** Ни одна тройка кандидатов не прошла sanity-check шаблона Ra-226. */
        object NoRa226Pattern : Result()

        /** Полином сошёлся, но коэффициенты вышли за разумные пределы (например, A слишком
         *  большое — спектр сильно нелинейный, или B<=0 — порядок каналов перепутан). */
        data class UnreasonableFit(val reason: String) : Result()
    }

    /** Один найденный пик с привязкой к ожидаемой энергии Ra-226. */
    data class IdentifiedPeak(
        /** Уточнённая центроидом позиция пика, в каналах (sub-channel точность). */
        val channel: Float,
        /** Ожидаемая энергия из якорной тройки, кэВ. */
        val expectedEnergyKev: Int,
        /** Сумма счётов в окне ±1·FWHM вокруг центроида (для оценки значимости). */
        val countsInWindow: Int,
        /** Эффективная FWHM в каналах для данного пика (масштаб окна центроида). */
        val fwhmChannels: Float,
    )

    /** Кандидат на пик из локального максимума сглаженного спектра. */
    private data class PeakCandidate(
        val channel: Int,
        val height: Double,
        val prominence: Double,
    )

    // ───────────────────────────── Main API ─────────────────────────────

    /**
     * Запускает идентификацию пиков Ra-226 и расчёт полинома.
     *
     * Не модифицирует никаких глобальных полей; вызывающий код сам решает, что делать
     * с [Result.Ok] (показать пользователю, записать в прибор и т. п.).
     *
     * @param spectrum сырой спектр (DoubleArray размером 1024/2048/4096); типично — копия
     *   `GO.drawSPECTER.spectrData`, чтобы пользователь не мог его модифицировать в процессе.
     * @param channels число каналов спектра. Не используется размер массива напрямую,
     *   потому что фактический размер `spectrData` может быть больше реального разрешения.
     * @return [Result] с готовым полиномом или причиной отказа.
     */
    fun analyze(spectrum: DoubleArray, channels: Int): Result {
        require(channels in 1024..4096) { "Unsupported channels: $channels" }
        require(spectrum.size >= channels) { "Spectrum too short: ${spectrum.size} < $channels" }

        // Шаг 1. Сгладить SMA-фильтром. Окно ~ FWHM/4 у пика 609 кэВ:
        //   - при 4096 каналах FWHM~50 → окно 12
        //   - при 2048 каналах FWHM~25 → окно 6
        //   - при 1024 каналах FWHM~12 → окно 3
        val smaWindow = when (channels) { 4096 -> 12; 2048 -> 6; else -> 3 }
        val smoothed = movingAverage(spectrum, channels, smaWindow)

        // Шаг 2. Оценить фон скользящим минимумом с окном ~6·FWHM. Фон — медленно
        // меняющаяся подложка, конкретный пик в окне минимизирует не саму себя.
        val bgWindow = smaWindow * 24
        val background = rollingMinimum(smoothed, bgWindow)

        // Шаг 3. Найти кандидаты в пики: локальные максимумы значимо выше фона.
        val candidates = findPeakCandidates(smoothed, background, channels, smaWindow)
        android.util.Log.i("BluZ-AutoCalib", "Peak candidates: ${candidates.size} found")
        candidates.sortedByDescending { it.prominence }.take(15).forEachIndexed { idx, c ->
            android.util.Log.i("BluZ-AutoCalib",
                "  [$idx] ch=${c.channel} height=${"%.0f".format(c.height)} prom=${"%.0f".format(c.prominence)}")
        }
        if (candidates.size < 3) return Result.NoRa226Pattern

        // Шаг 4. Pattern matching: перебрать все тройки кандидатов, для каждой решить
        // систему как (ch_i, 82), (ch_j, 352), (ch_k, 609) и проверить sanity.
        val best = findBestTripletByPattern(candidates, channels)
        val anchorsForRefine: List<Int>
        val anchorChannels: List<Int>
        val baseCoefs: Triple<Float, Float, Float>

        if (best != null) {
            android.util.Log.i("BluZ-AutoCalib",
                "Best triplet (quadratic): ch=${best.tripletChannels} A=${best.cA} B=${best.cB} C=${best.cC} score=${best.score}")
            anchorChannels = best.tripletChannels
            anchorsForRefine = anchorEnergiesKev.toList()
            baseCoefs = Triple(best.cA, best.cB, best.cC)
        } else {
            // Fallback на пару 352/609 — линейная калибровка. Применяется когда пик 82 кэВ
            // не различим (компараторный фон высокий, или источник слабый). На маленьком
            // детекторе обе линии Pb-214 и Bi-214 видны почти всегда, в отличие от 82.
            android.util.Log.w("BluZ-AutoCalib", "No triplet passed sanity check, trying pair fallback (352/609)…")
            val pair = findBestPairByPattern(candidates, channels)
            if (pair == null) {
                android.util.Log.w("BluZ-AutoCalib", "No pair passed sanity check either")
                return Result.NoRa226Pattern
            }
            android.util.Log.i("BluZ-AutoCalib",
                "Best pair (linear): ch=${pair.first}/${pair.second} → B=${pair.third.second} C=${pair.third.third}")
            anchorChannels = listOf(pair.first, pair.second)
            anchorsForRefine = fallbackAnchorPair.toList()
            baseCoefs = pair.third
        }

        // Шаг 5. Уточнить центроиды якорных пиков по сглаженному спектру.
        val refined: List<IdentifiedPeak> = anchorChannels.zip(anchorsForRefine)
            .map { (ch, e) ->
                val fwhmCh = expectedFwhmChannels(e, baseCoefs.first, baseCoefs.second, baseCoefs.third, channels)
                val centroid = refineCentroid(smoothed, background, ch.toDouble(), fwhmCh)
                val cnt = sumCountsInWindow(spectrum, background, centroid, fwhmCh)
                IdentifiedPeak(centroid.toFloat(), e, cnt, fwhmCh.toFloat())
            }

        // Шаг 6. Минимальная значимость — каждый якорный пик должен набрать достаточно счётов.
        // **Послабление для 82 кэВ:** на маленьком детекторе пик 82 кэВ ярчайший, но при
        // компараторном фоне его «обрезает» снизу; считаем достаточным MIN/2.
        for (p in refined) {
            val minCounts = if (p.expectedEnergyKev <= 100) MIN_COUNTS_PER_PEAK / 2 else MIN_COUNTS_PER_PEAK
            if (p.countsInWindow < minCounts) {
                android.util.Log.w("BluZ-AutoCalib", "Weak peak: ${p.expectedEnergyKev} keV has only ${p.countsInWindow} counts (need ≥ $minCounts)")
                return Result.WeakSignal
            }
        }

        // Шаг 7. Финальное решение по уточнённым центроидам.
        // Для тройки — квадратичный полином, для пары — линейный (через две точки, A=0).
        val (finalA, finalB, finalC) = if (refined.size == 3) {
            val mtrx = Mtrx()
            mtrx.sysArray[0][0] = refined[0].channel.toDouble()
            mtrx.sysArray[0][1] = refined[0].expectedEnergyKev.toDouble()
            mtrx.sysArray[1][0] = refined[1].channel.toDouble()
            mtrx.sysArray[1][1] = refined[1].expectedEnergyKev.toDouble()
            mtrx.sysArray[2][0] = refined[2].channel.toDouble()
            mtrx.sysArray[2][1] = refined[2].expectedEnergyKev.toDouble()
            mtrx.sysEq()
            if (!mtrx.solved) return Result.UnreasonableFit("система коэффициентов вырождена")
            Triple(mtrx.cA, mtrx.cB, mtrx.cC)
        } else {
            // Линейный fit по двум точкам: A = 0, B = ΔE/Δch, C = E1 - B·ch1
            val ch1 = refined[0].channel.toDouble(); val e1 = refined[0].expectedEnergyKev.toDouble()
            val ch2 = refined[1].channel.toDouble(); val e2 = refined[1].expectedEnergyKev.toDouble()
            val b = ((e2 - e1) / (ch2 - ch1)).toFloat()
            val c = (e1 - b * ch1).toFloat()
            Triple(0.0f, b, c)
        }

        // Sanity финального полинома.
        val sanity = checkPolynomialSanity(finalA, finalB, finalC, channels)
        if (sanity != null) return Result.UnreasonableFit(sanity)

        // RMS-невязка предсказанных энергий vs ожидаемых.
        val residualKev = sqrt(refined.sumOf { p ->
            val predicted = finalA * p.channel * p.channel + finalB * p.channel + finalC
            val diff = predicted - p.expectedEnergyKev
            (diff * diff).toDouble()
        } / refined.size).toFloat()

        return Result.Ok(finalA, finalB, finalC, refined, residualKev)
    }

    /**
     * Линейная калибровка по паре **82 + 609 кэВ**. На маленьком детекторе:
     *  - 82 кэВ — ярчайший пик в спектре (рентгены свинца)
     *  - 609 кэВ — ослаблен в 4–20 раз от 82, дальше справа от 82
     *
     * **Критерии выбора правого якоря (609):**
     *  - prominence в диапазоне `[leftAnchor.prom × 5%, leftAnchor.prom × 30%]`
     *    (на маленьком детекторе пик 609 кэВ имеет именно такое относительное отношение
     *    к пику 82; меньше 5% — это уже комптоновский фон, больше 30% — это сам 82 или
     *    близкий к нему пик)
     *  - расположен **правее** левого якоря (channel > leftAnchor.channel + 30)
     *  - из всех подходящих — выбираем **самый правый**
     *
     * Без жёсткой границы `midCh`: при сильно сжатом спектре пик 609 может оказаться
     * в первой трети каналов, и жёсткое ограничение приводило к промаху.
     *
     * Возвращает `Triple(ch_left, ch_right, (0, B, C))`.
     */
    private fun findBestPairByPattern(
        candidates: List<PeakCandidate>, channels: Int
    ): Triple<Int, Int, Triple<Float, Float, Float>>? {
        if (candidates.size < 2) return null

        // Левый якорь = ярчайший пик в спектре (на маленьком детекторе это 82 кэВ).
        val leftAnchor = candidates.maxByOrNull { it.prominence } ?: return null
        val leftProm = leftAnchor.prominence

        // Кандидаты на правый якорь: prominence в диапазоне 5–30% от leftAnchor,
        // канал > leftAnchor.channel + 30. Берём самый правый.
        val rightCandidates = candidates.filter {
            it.channel > leftAnchor.channel + 30 &&
            it.prominence in (leftProm * 0.05)..(leftProm * 0.30)
        }
        val rightAnchor = rightCandidates.maxByOrNull { it.channel }
        if (rightAnchor == null) {
            android.util.Log.w("BluZ-AutoCalib",
                "Pair: no right anchor found (left=${leftAnchor.channel} prom=${leftProm.toInt()}, " +
                "need prom in [${(leftProm * 0.05).toInt()}..${(leftProm * 0.30).toInt()}] right of ch=${leftAnchor.channel + 30})")
            return null
        }

        val e1 = fallbackAnchorPair[0].toFloat()  // 82
        val e2 = fallbackAnchorPair[1].toFloat()  // 609
        val b = (e2 - e1) / (rightAnchor.channel - leftAnchor.channel)
        val c = e1 - b * leftAnchor.channel
        android.util.Log.i("BluZ-AutoCalib",
            "Pair fit: left=${leftAnchor.channel} (prom=${leftAnchor.prominence.toInt()}) right=${rightAnchor.channel} (prom=${rightAnchor.prominence.toInt()}) → B=$b C=$c")
        if (checkPolynomialSanity(0.0f, b, c, channels) != null) {
            android.util.Log.w("BluZ-AutoCalib", "Pair fit fails sanity: B=$b C=$c")
            return null
        }
        return Triple(leftAnchor.channel, rightAnchor.channel, Triple(0.0f, b, c))
    }

    // ───────────────────────────── Шаги алгоритма ─────────────────────────────

    /** Скользящее среднее с окном `2·radius+1`. По краям массива окно усекается. */
    private fun movingAverage(src: DoubleArray, n: Int, radius: Int): DoubleArray {
        val out = DoubleArray(n)
        var sum = 0.0
        var cnt = 0
        for (i in 0 until min(radius + 1, n)) { sum += src[i]; cnt++ }
        out[0] = sum / cnt
        for (i in 1 until n) {
            val addIdx = i + radius
            val remIdx = i - radius - 1
            if (addIdx < n) { sum += src[addIdx]; cnt++ }
            if (remIdx >= 0) { sum -= src[remIdx]; cnt-- }
            out[i] = if (cnt > 0) sum / cnt else 0.0
        }
        return out
    }

    /**
     * Скользящий минимум с окном `2·radius+1` — служит грубой оценкой фоновой подложки
     * (для широких окон 6·FWHM минимум сидит в «долинах» между пиками, не на самих пиках).
     * Реализация O(n·w), для наших размеров (≤4096) этого хватает.
     */
    private fun rollingMinimum(src: DoubleArray, radius: Int): DoubleArray {
        val n = src.size
        val out = DoubleArray(n)
        for (i in 0 until n) {
            val lo = max(0, i - radius)
            val hi = min(n - 1, i + radius)
            var m = src[lo]
            for (j in lo + 1..hi) if (src[j] < m) m = src[j]
            out[i] = m
        }
        return out
    }

    /**
     * Кандидаты в пики — локальные максимумы сглаженного спектра, удовлетворяющие:
     *  - `s[i] >= s[i±1..i±halfWindow]` — действительно максимум в окрестности
     *  - `s[i] - bg[i] > 3·√bg[i]` — значимо выше фона (3σ от пуассоновской статистики)
     *  - не в первых/последних 20 каналах (там артефакты компаратора и обрезка)
     *
     * Возвращает все кандидаты с их prominence (превышением над фоном); вызывающий
     * код сам сортирует/фильтрует.
     */
    private fun findPeakCandidates(
        smoothed: DoubleArray, bg: DoubleArray, n: Int, halfWindow: Int
    ): List<PeakCandidate> {
        val out = mutableListOf<PeakCandidate>()
        val edge = 20
        var i = edge
        while (i < n - edge) {
            val v = smoothed[i]
            val b = bg[i]
            val prominence = v - b
            // Пороговый критерий 3·√bg: для пуассона стандартное отклонение фона == √bg.
            if (prominence > 3.0 * sqrt(max(1.0, b))) {
                // Проверка локального максимума в окрестности.
                var isMax = true
                val lo = max(edge, i - halfWindow)
                val hi = min(n - edge - 1, i + halfWindow)
                for (j in lo..hi) {
                    if (j != i && smoothed[j] > v) { isMax = false; break }
                }
                if (isMax) {
                    out.add(PeakCandidate(i, v, prominence))
                    i += halfWindow  // защита от двойной регистрации одного пика
                }
            }
            i++
        }
        return out
    }

    /** Результат успешного pattern matching: каналы трёх якорных пиков и грубый полином. */
    private data class TripletMatch(
        val tripletChannels: List<Int>,
        val cA: Float,
        val cB: Float,
        val cC: Float,
        val score: Double,
    )

    /**
     * Перебирает все тройки `(ci, cj, ck)` из топ-20 кандидатов (по prominence), для каждой
     * решает систему как (ci=82, cj=352, ck=609 кэВ — см. [anchorEnergiesKev]), проверяет
     * sanity полинома через [checkPolynomialSanity] и считает **score** — суммарное
     * «согласие» полинома с реальностью:
     *
     *  - +1 за каждую из [auxEnergiesKev] (186/242/295 кэВ), в окрестности которой
     *    реально есть пик-кандидат в окне ±1·FWHM
     *  - микро-бонус (×1e-6) за суммарную prominence самой тройки — разрешает spread
     *    между равно-score'ыми вариантами в пользу более ярких якорей
     *
     * **Перебор C(20, 3) = 1140 вариантов** выполняется за единицы миллисекунд даже на
     * старых телефонах: каждая итерация — три арифметические операции (Mtrx.sysEq), плюс
     * O(|auxEnergiesKev|) на оценку score.
     *
     * Возвращает лучшую тройку. Если ни одна не проходит sanity → `null`; вызывающий
     * код переключается на pair fallback ([findBestPairByPattern]).
     */
    private fun findBestTripletByPattern(
        candidates: List<PeakCandidate>, channels: Int
    ): TripletMatch? {
        // take(20) вместо take(10): пик 609 кэВ на маленьком детекторе бывает в 20-40
        // раз слабее пика 82, может не попасть в топ-10 по prominence. С 20 кандидатами
        // перебор всех троек = C(20,3) = 1140, всё ещё быстро.
        val top = candidates.sortedByDescending { it.prominence }.take(20).sortedBy { it.channel }
        if (top.size < 3) return null

        var best: TripletMatch? = null
        for (i in 0 until top.size - 2) {
            for (j in i + 1 until top.size - 1) {
                for (k in j + 1 until top.size) {
                    val ci = top[i].channel
                    val cj = top[j].channel
                    val ck = top[k].channel
                    if (cj - ci < 5 || ck - cj < 5) continue  // дубликаты/слишком близко

                    val mtrx = Mtrx()
                    mtrx.sysArray[0][0] = ci.toDouble(); mtrx.sysArray[0][1] = anchorEnergiesKev[0].toDouble()
                    mtrx.sysArray[1][0] = cj.toDouble(); mtrx.sysArray[1][1] = anchorEnergiesKev[1].toDouble()
                    mtrx.sysArray[2][0] = ck.toDouble(); mtrx.sysArray[2][1] = anchorEnergiesKev[2].toDouble()
                    mtrx.sysEq()
                    if (!mtrx.solved) continue
                    val sanityReason = checkPolynomialSanity(mtrx.cA, mtrx.cB, mtrx.cC, channels)
                    if (sanityReason != null) {
                        android.util.Log.d("BluZ-AutoCalib", "Triplet rejected ($ci,$cj,$ck): $sanityReason")
                        continue
                    }

                    // Score: сколько вспомогательных линий Ra-226 находят пик-кандидата
                    // в предсказанной по полиному позиции, в окне ±1·FWHM.
                    var score = 0.0
                    for (auxE in auxEnergiesKev) {
                        val predCh = predictChannel(auxE, mtrx.cA, mtrx.cB, mtrx.cC) ?: continue
                        val fwhmCh = expectedFwhmChannels(auxE, mtrx.cA, mtrx.cB, mtrx.cC, channels)
                        val nearest = candidates.minByOrNull { abs(it.channel - predCh) } ?: continue
                        if (abs(nearest.channel - predCh) <= fwhmCh) score += 1.0
                    }
                    // Бонус за высокую суммарную prominence самой тройки — отсеивает варианты,
                    // где якорные пики слабее вспомогательных шумов.
                    score += (top[i].prominence + top[j].prominence + top[k].prominence) * 1e-6

                    if (best == null || score > best.score) {
                        best = TripletMatch(listOf(ci, cj, ck), mtrx.cA, mtrx.cB, mtrx.cC, score)
                    }
                }
            }
        }
        return best
    }

    /**
     * Уточняет позицию пика взвешенным центроидом в окне ±0.7·FWHM вокруг грубого
     * максимума. Даёт sub-channel точность и устойчивость к статистическому шуму.
     *
     *   centroid = Σ ch·(s[ch] − bg[ch]) / Σ (s[ch] − bg[ch])
     *
     * @param roughChannel грубое положение пика (центр локального максимума).
     * @param fwhmChannels ожидаемая FWHM в каналах при текущем разрешении.
     */
    private fun refineCentroid(
        smoothed: DoubleArray, bg: DoubleArray, roughChannel: Double, fwhmChannels: Double
    ): Double {
        val n = smoothed.size
        val radius = (0.7 * fwhmChannels).toInt().coerceAtLeast(2)
        val center = roughChannel.toInt().coerceIn(radius, n - radius - 1)
        var num = 0.0
        var den = 0.0
        for (ch in center - radius..center + radius) {
            val w = max(0.0, smoothed[ch] - bg[ch])
            num += ch * w
            den += w
        }
        return if (den > 0.0) num / den else roughChannel
    }

    /** Сумма (s − bg) в окне ±1·FWHM. Используется как метрика значимости пика. */
    private fun sumCountsInWindow(
        spectrum: DoubleArray, bg: DoubleArray, center: Double, fwhmChannels: Double
    ): Int {
        val n = spectrum.size
        val radius = fwhmChannels.toInt().coerceAtLeast(2)
        val c = center.toInt().coerceIn(radius, n - radius - 1)
        var sum = 0.0
        for (ch in c - radius..c + radius) sum += max(0.0, spectrum[ch] - bg[ch])
        return sum.toInt()
    }

    /**
     * Для известной калибровки [cA, cB, cC] решает обратное уравнение `ch(E)` — возвращает
     * канал, в котором ожидается пик энергии [energyKev]. Используется для:
     *  - верификации тройки через вспомогательные линии Ra-226 ([auxEnergiesKev])
     *  - расчёта канала компаратора по минимальной полезной энергии
     *
     * Решение: `A·ch² + B·ch + (C − E) = 0`. При A ≈ 0 — линейное приближение `ch = (E − C)/B`.
     * @return канал или `null` если дискриминант отрицательный или решение вне разумных пределов.
     */
    fun predictChannel(energyKev: Int, cA: Float, cB: Float, cC: Float): Double? {
        // Линейный путь — самый частый случай для нашего прибора (A в районе 1e-6).
        if (abs(cA) < 1e-9f) {
            if (cB == 0.0f) return null
            return ((energyKev - cC) / cB).toDouble()
        }
        val a = cA.toDouble()
        val b = cB.toDouble()
        val c = (cC - energyKev).toDouble()
        val disc = b * b - 4 * a * c
        if (disc < 0) return null
        val sqrtD = sqrt(disc)
        // Берём положительный корень: канал всегда ≥ 0.
        val r1 = (-b + sqrtD) / (2 * a)
        val r2 = (-b - sqrtD) / (2 * a)
        return when {
            r1 >= 0 && r2 >= 0 -> min(r1, r2)
            r1 >= 0 -> r1
            r2 >= 0 -> r2
            else -> null
        }
    }

    /**
     * Ожидаемая FWHM в каналах для данной энергии. Модель: относительная FWHM
     * сцинтиллятора `≈ 0.08·√(662/E)` (статистика числа сцинтилляционных фотонов),
     * абсолютная — `FWHM_kev = E · relFwhm`. Перевод в каналы — через производную полинома.
     */
    private fun expectedFwhmChannels(
        energyKev: Int, cA: Float, cB: Float, cC: Float, channels: Int
    ): Double {
        val relFwhm = 0.08 * sqrt(662.0 / energyKev.coerceAtLeast(1))
        val fwhmKev = energyKev * relFwhm
        // dE/dch = 2·A·ch + B. Для канала пика 609 кэВ это примерно B.
        val chAt = predictChannel(energyKev, cA, cB, cC) ?: (channels / 2.0)
        val dE_dCh = 2.0 * cA * chAt + cB
        if (dE_dCh <= 0) return (channels * 0.02)  // fallback: 2% от полного диапазона
        return fwhmKev / dE_dCh
    }

    /**
     * Проверка «разумности» полинома. Возвращает `null` если коэффициенты в норме,
     * иначе — строку с описанием проблемы. Пороги подобраны эмпирически для прибора
     * с диапазоном 0–3500 кэВ на 1024/2048/4096 каналов.
     */
    private fun checkPolynomialSanity(cA: Float, cB: Float, cC: Float, channels: Int): String? {
        // **Монотонность.** Энергия должна расти с номером канала на всём диапазоне.
        // Производная E'(ch) = 2·A·ch + B; минимум на конце шкалы ch=channels-1.
        // Если в каком-то канале E'(ch) ≤ 0, полином немонотонный — нефизично,
        // даже если численно описывает 3 заданные точки. Это типичная ловушка
        // квадратичной подгонки на тройке близких к линейности пиков.
        val derivAtEnd = 2f * cA * (channels - 1) + cB
        if (derivAtEnd <= 0f) {
            return "полином немонотонный (производная на ch=${channels - 1} = ${"%.3f".format(derivAtEnd)})"
        }
        // |A| ограничен сверху. Реальная нелинейность маленького сцинтиллятора на низких
        // энергиях даёт A до ~2e-3 (натянуто шкала 0..3500 кэВ при правильной идентификации
        // тройки 82/352/609 — A ≈ 1.5e-3). Порог 5e-3 пропускает реальную физику,
        // отбраковывает явно нефизичные полиномы.
        if (abs(cA) > 0.005f) return "коэф. A=${"%.6f".format(cA)} вне нормы (ожидаемо ≤ 5e-3)"
        // B в коридоре ×0.2..×5 от ожидаемого 3500/channels.
        val expectedB = TARGET_FULL_RANGE_KEV.toFloat() / channels
        if (cB < expectedB * 0.2f || cB > expectedB * 5.0f) {
            return "коэф. B=${"%.3f".format(cB)} вне нормы (ожидаемо ~${"%.3f".format(expectedB)})"
        }
        // C — допускаем ±300 кэВ.
        if (abs(cC) > 300f) return "коэф. C=${"%.1f".format(cC)} вне нормы"
        return null
    }
}
