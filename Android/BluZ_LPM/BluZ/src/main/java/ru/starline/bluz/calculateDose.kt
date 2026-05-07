package ru.starline.bluz
/**
 * Расчёт эквивалентной дозы H*(10) по измеренному спектру.
 */
object DoseCalculator {

    public var chiVectorOrg: DoubleArray = DoubleArray(1024)
    /**
     * @param chi Вектор весовых коэффициентов (размер строго 1024).
     *            Единицы: [Доза]/[Импульс] (например, пЗв/имп).
     * @param spectrum Измеренный спектр (1024, 2048 или 4096 каналов).
     * @param normalize Если true, спектр нормируется на единицу площади перед расчётом.
     *                  Используйте false, если chi уже откалиброван под абсолютные отсчёты.
     * @return Значение дозы в тех же единицах, что и chi.
     */
    fun calculateH10Dose(
        chi: DoubleArray,
        spectrum: DoubleArray,
        normalize: Boolean = false
    ): Double {
        require(chi.size == 1024) { "chi должен содержать ровно 1024 элемента" }
        require(spectrum.size % 1024 == 0) {
            "Размер спектра должен быть кратен 1024 (1024, 2048, 4096)"
        }

        val ratio = spectrum.size / 1024
        var dose = 0.0
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

        // Нормировка, если требуется
        return if (normalize && spectrumSum > 0.0) dose / spectrumSum else dose
    }

    /**
     * Безопасный расчёт с проверкой на отрицательные значения и NaN.
     * Полезно для работы с сырыми данными АЦП.
     */
    fun calculateH10DoseSafe(
        chi: DoubleArray,
        spectrum: DoubleArray,
        normalize: Boolean = false
    ): Double {
        // Очищаем спектр от артефактов АЦП
        val cleanedSpectrum = DoubleArray(spectrum.size) { idx ->
            val v = spectrum[idx]
            if (v < 0.0 || v.isNaN()) 0.0 else v
        }
        return calculateH10Dose(chi, cleanedSpectrum, normalize)
    }
}