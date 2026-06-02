package ru.starline.bluz

import android.util.Log

/**
 * Решение системы трёх линейных уравнений методом определителей Крамера для нахождения
 * коэффициентов квадратного полинома энергетической калибровки спектра:
 *
 * `E(ch) = A·ch² + B·ch + C`
 *
 * **Чистая функция:** [sysEq] заполняет только собственные поля [cA], [cB], [cC] и
 * флаг [solved]. **Никаких побочных эффектов** — в `GO.propCoef*` не пишет; запись
 * остаётся обязанностью вызывающего кода (он лучше знает, в какое разрешение
 * писать и что делать при `solved == false`).
 *
 * **Использование:**
 *  1. Заполнить `sysArray[i][0] = канал`, `sysArray[i][1] = энергия` (3 пары точек)
 *  2. Вызвать [sysEq]
 *  3. Если `solved == true` — взять [cA], [cB], [cC]
 *  4. Если `solved == false` — система вырождена (точки коллинеарны), результат
 *     невалидный; вызывающий код должен либо отказаться от записи, либо использовать
 *     дефолт (A=0, B=1, C=0 — идентичная функция) на своё усмотрение.
 */
class Mtrx {
    public var cA: Float = 0.0f
    public var cB: Float = 1.0f
    public var cC: Float = 0.0f

    /** `true` если последний вызов [sysEq] нашёл решение; `false` если главный
     *  определитель оказался нулевым (точки коллинеарны или совпадают). */
    public var solved: Boolean = false

    public var sysArray: Array<DoubleArray> = Array(3) { DoubleArray(2) }

    /** Вычисляет определитель матрицы 3×3 по правилу Саррюса. */
    private fun determinant(matr: Array<DoubleArray>): Double {
        val det =
            matr[0][0] * matr[1][1] * matr[2][2] +
            matr[2][0] * matr[0][1] * matr[1][2] +
            matr[1][0] * matr[2][1] * matr[0][2] -
            matr[2][0] * matr[1][1] * matr[0][2] -
            matr[0][0] * matr[2][1] * matr[1][2] -
            matr[1][0] * matr[0][1] * matr[2][2]
        return det
    }

    /**
     * Решает систему 3 уравнений относительно A, B, C методом Крамера:
     * ```
     * E1 = A·ch1² + B·ch1 + C
     * E2 = A·ch2² + B·ch2 + C
     * E3 = A·ch3² + B·ch3 + C
     * ```
     * где `sysArray[i] = (chi, Ei)`.
     *
     * При `mainDet == 0` (вырожденная система) выставляет [solved] = false и сбрасывает
     * [cA] = 0, [cB] = 1, [cC] = 0. В `GO` ничего не пишет — см. KDoc класса.
     */
    public fun sysEq() {
        val detArray: Array<DoubleArray> = Array(3) { DoubleArray(3) }
        detArray[0][0] = sysArray[0][0] * sysArray[0][0]
        detArray[1][0] = sysArray[1][0] * sysArray[1][0]
        detArray[2][0] = sysArray[2][0] * sysArray[2][0]
        detArray[0][1] = sysArray[0][0]
        detArray[1][1] = sysArray[1][0]
        detArray[2][1] = sysArray[2][0]
        detArray[0][2] = 1.0
        detArray[1][2] = 1.0
        detArray[2][2] = 1.0

        val mainDet = determinant(detArray)
        if (mainDet == 0.0) {
            cA = 0.0f
            cB = 1.0f
            cC = 0.0f
            solved = false
            Log.w("BluZ-BT", "Mtrx.sysEq: mainDet==0, system degenerate")
            return
        }

        detArray[0][0] = sysArray[0][1]
        detArray[1][0] = sysArray[1][1]
        detArray[2][0] = sysArray[2][1]
        cA = (determinant(detArray) / mainDet).toFloat()

        detArray[0][0] = sysArray[0][0] * sysArray[0][0]
        detArray[1][0] = sysArray[1][0] * sysArray[1][0]
        detArray[2][0] = sysArray[2][0] * sysArray[2][0]
        detArray[0][1] = sysArray[0][1]
        detArray[1][1] = sysArray[1][1]
        detArray[2][1] = sysArray[2][1]
        cB = (determinant(detArray) / mainDet).toFloat()

        detArray[0][1] = sysArray[0][0]
        detArray[1][1] = sysArray[1][0]
        detArray[2][1] = sysArray[2][0]
        detArray[0][2] = sysArray[0][1]
        detArray[1][2] = sysArray[1][1]
        detArray[2][2] = sysArray[2][1]
        cC = (determinant(detArray) / mainDet).toFloat()

        solved = true
        Log.i("BluZ-BT", "Mtrx.sysEq: det=$mainDet A=$cA B=$cB C=$cC")
    }
}
