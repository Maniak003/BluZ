package ru.starline.bluz

import android.util.Log

/**
 * Расчёт эквивалентной дозы H*(10) по измеренному спектру через скалярное произведение
 * с весовым χ-вектором детектора.
 *
 * χ-вектор — энергозависимая чувствительность конкретного детектора (NaI, CsI, ...) в единицах
 * `[μR/h] / [импульс]`. Хранится в Room в таблице `detectors` ([data.entity.DetectorType]),
 * грузится в [chiVectorOrg] при выборе активного детектора.
 *
 * **Алгоритм** ([calculateH10Dose]):
 *  - Ребиннинг спектра до 1024 каналов (если ResolutionSpectr=2048 → ratio=2, =4096 → ratio=4)
 *  - Скалярное произведение `Σ chi[i] · spectrum[i*ratio..i*ratio+ratio-1]`
 *  - Опционально — нормировка на сумму спектра
 *  - Опционально — пересчёт в мощность дозы через `acquisitionTimeSec`
 *
 * **Безопасный вариант** [calculateH10DoseSafe] — дополнительно чистит NaN и отрицательные
 * значения АЦП. Используется в [drawSpecter.redrawSpecter] для расчёта MLEM-компенсированной
 * мощности дозы (`GO.compMED`).
 */
object DoseCalculator {

    public var chiVectorOrg: DoubleArray = DoubleArray(1024)
    /**
     * @param chi Вектор весовых коэффициентов (размер строго 1024).
     *            Единицы: [мкР/ч]/[Импульс].
     * @param spectrum Измеренный спектр (1024, 2048 или 4096 каналов).
     * @param normalize Если true, спектр нормируется на единицу площади перед расчётом.
     *                  Используйте false, если chi уже откалиброван под абсолютные отсчёты.
     * @param acquisitionTimeSec Время набора спектра
     * @param exnergyMaxSpecter Максимальная энергия в спектре. Определяется калибровкой.
     * @param energyMaxCHI Максимальная энекгия в векторе. Обычно расчет делаю до 3.5Мэв.
     * @return Значение дозы в тех же единицах, что и chi.
     */
    /**
     * Базовый расчёт эквивалентной дозы H*(10).
     *
     * Алгоритм:
     *  1. `ratio = spectrum.size / 1024` — коэффициент ребиннинга
     *  2. Цикл по 1024 каналам χ: для каждого `binSum = Σ spectrum[i*ratio..i*ratio+ratio-1]`
     *  3. `dose = Σ chi[i] · binSum`
     *  4. Опционально: `dose /= spectrumSum` (нормировка)
     *  5. Опционально: `dose = dose / acquisitionTimeSec * 3600` (мощность дозы)
     *
     * @param chi χ-вектор (размер ровно 1024).
     * @param spectrum Спектр (размер должен быть кратен 1024: 1024 / 2048 / 4096).
     * @param normalize Нормировать ли спектр на единицу площади.
     * @param acquisitionTimeSec Время измерения; если > 0 — возвращается мощность дозы (на час).
     * @param exnergyMaxSpecter Макс. энергия спектра, кэВ (резерв на будущее, не используется).
     * @param energyMaxCHI Макс. энергия χ-вектора (резерв).
     * @return Доза или мощность дозы в единицах χ (обычно мкР/ч).
     * @throws IllegalArgumentException если размеры не подходят.
     */
    fun calculateH10Dose(
        chi: DoubleArray,
        spectrum: DoubleArray,
        normalize: Boolean = false,
        acquisitionTimeSec: Double = 0.0,
        exnergyMaxSpecter:Float = 4.5f,
        energyMaxCHI:Float = 3.5f
    ): Double {
        require(chi.size == 1024) { "chi size is 1024." }
        require(spectrum.size % 1024 == 0) { "Size multiplay 1024 (1024, 2048, 4096)" }
        var dose: Double = 0.0

        //val dChan = energyMaxCHI / exnergyMaxSpecter * 1024;
        //Log.d("BluZ-BT", "dChan: $dChan")
        //Log.d("BluZ-BT", "nChi: $nChi, nSpec: $nSpec, dEChi: $dEChi, dESpec: $dESpec, dose: $dose, exnergyMaxSpecter: $exnergyMaxSpecter");

        val ratio = spectrum.size / 1024
        var spectrumSum = 0.0

        // Ребиннинг + скалярное произведение в одном проходе
        for (i in chi.indices) {
            var binSum = 0.0
            // Суммируем ratio соседних каналов спектра в один канал chi
            for (j in 0 until ratio) {
                binSum += spectrum[i * ratio + j]
            }
            spectrumSum += binSum
            dose += chi[i] * binSum
        }
        // Нормировка спектра (если нужно)
        if (normalize && spectrumSum > 0.0) {
            dose /= spectrumSum
        }

        // Пересчёт в мощность дозы (если задано время)
        return if (acquisitionTimeSec > 0.0) {
            dose / acquisitionTimeSec * 3600.0  // [доза/час]
        } else {
            dose  // [доза] за всё время измерения
        }
    }

    /**
     * Безопасный расчёт с проверкой на отрицательные значения и NaN.
     * Полезно для работы с сырыми данными АЦП.
     */
    /**
     * Безопасная обёртка над [calculateH10Dose]: предварительно чистит сырой спектр от
     * `NaN` и отрицательных значений (артефакты АЦП → 0).
     *
     * Возвращает `0` если `exnergyMaxSpecter <= 0` — означает что калибровка не задана,
     * MLEM-расчёт не имеет смысла.
     */
    fun calculateH10DoseSafe(
        chi: DoubleArray,
        spectrum: DoubleArray,
        normalize: Boolean = false,
        acquisitionTimeSec: Double = 0.0,
        exnergyMaxSpecter:Float = 4.5f
    ): Double {
        if (exnergyMaxSpecter > 0.0f) {
            // Очищаем спектр от артефактов АЦП
            val cleanedSpectrum = DoubleArray(spectrum.size) { idx ->
                val v = spectrum[idx]
                if (v < 0.0 || v.isNaN()) 0.0 else v
            }
            return calculateH10Dose(
                chi,
                cleanedSpectrum,
                normalize,
                acquisitionTimeSec,
                exnergyMaxSpecter
            )
        } else {
            return 0.0
        }
    }
}