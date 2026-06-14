package ru.starline.bluz

import android.util.Log
import kotlin.math.abs
import kotlin.math.pow
import org.apache.commons.math3.fitting.PolynomialCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoints
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Created by ed on 03,июнь,2026
 */
class energyCalculator {

    private val _calibrationPoints = mutableListOf<Pair<Double, Double>>()

    // Публичный API для добавления точки
    fun addCalibrationPoint(channel: Double, energy: Double) {
        // Опционально: проверка на дубликаты по каналу
        if (_calibrationPoints.none { abs(it.first - channel) < 1e-6 }) {
            _calibrationPoints.add(channel to energy)
        }
    }

    // Очистка (например, при сбросе калибровки)
    fun clearCalibration() = _calibrationPoints.clear()

    // Количество точек
    val pointCount: Int get() = _calibrationPoints.size

    // Геттер для использования в фиттере
    fun getCalibrationPoints(): List<Pair<Double, Double>> = _calibrationPoints.toList()

    data class CalibrationResult(
        val coefficients: DoubleArray,  // [a0, a1, a2, a3, a4]
        val rmsError: Double,           // среднеквадратичная ошибка
        val isValid: Boolean
    )

    /* Коэффициенты полинома */
    public var pA: Float = 0f
    public var pB: Float = 0f
    public var pC: Float = 0f
    public var pD: Float = 0f
    public var pE: Float = 0f
    public var maxChan = 1023

    /* Разрешение спектра */
    public var rS: Int = 0

    public fun init (polA: Float, polB: Float, polC: Float, polD: Float, polE: Float, resol: Int) {
        pA = polA
        pB = polB
        pC = polC
        pD = polD
        pE = polE
        rS = resol
        maxChan = when(resol) {
            1 -> 2047
            2 -> 4095
            else -> 1023
        }
        Log.i("BluZ-BT", "pA: $pA, pB: $pB, pC: $pC, pD: $pD, pE: $pE, resol: $resol")
    }

    /**
     * Построение калибровочной кривой: энергия = f(канал)
     * @param points список пар (канал, энергия в кэВ)
     * @param degree степень полинома (обычно 2, 3 или 4)
     */
    fun fitCalibration(
        points: List<Pair<Double, Double>>,
        degree: Int = 4
    ): CalibrationResult {
        if(points.size != degree + 1) {
            Log.e("BluZ-BT", "Нужно минимум ${degree + 1} точек для полинома степени $degree")
            return (CalibrationResult(coefficients = DoubleArray(degree + 1), rmsError = Double.MAX_VALUE, isValid = false))
        }

        val observations = WeightedObservedPoints()
        for ((channel, energy) in points) {
            observations.add(channel, energy)  // вес = 1.0 по умолчанию
        }

        return try {
            val fitter = PolynomialCurveFitter.create(degree)
            val coefficients = fitter.fit(observations.toList())

            // Оценка качества: RMS ошибка
            var sumSquaredError = 0.0
            for ((ch, energyExpected) in points) {
                val energyCalc = evaluatePolynomial(coefficients, ch)
                val err = energyCalc - energyExpected
                sumSquaredError += err * err
            }
            val rmsError = sqrt(sumSquaredError / points.size)

            CalibrationResult(
                coefficients = coefficients,
                rmsError = rmsError,
                isValid = rmsError < 2.0  // порог: 2 кэВ — настройте под свою задачу
            )
        } catch (e: Exception) {
            CalibrationResult(
                coefficients = DoubleArray(degree + 1),
                rmsError = Double.MAX_VALUE,
                isValid = false
            )
        }
    }

    /* Вычисление значения полинома по коэффициентам (схема Горнера) */
    fun evaluatePolynomial(coefficients: DoubleArray, x: Double): Double {
        var result = 0.0
        for (i in coefficients.size - 1 downTo 0) {
            result = result * x + coefficients[i]
        }
        return result
    }

    /* Обратное преобразование: энергия → канал (бинарный поиск) */
    fun findChannel(
        coefficients: DoubleArray,
        targetEnergy: Double,
        channelMin: Double = 0.0,
        channelMax: Double = 16383.0,
        tolerance: Double = 1e-3,
        maxIter: Int = 60
    ): Double? {
        val eMin = evaluatePolynomial(coefficients, channelMin)
        val eMax = evaluatePolynomial(coefficients, channelMax)

        if (targetEnergy < minOf(eMin, eMax) - 1.0 ||
            targetEnergy > maxOf(eMin, eMax) + 1.0) {
            return null  // вне диапазона
        }

        var low = channelMin
        var high = channelMax

        repeat(maxIter) {
            val mid = (low + high) / 2.0
            val eMid = evaluatePolynomial(coefficients, mid)

            if (abs(eMid - targetEnergy) < tolerance) return mid

            if ((eMid < targetEnergy) == (eMin < eMax)) {
                low = mid
            } else {
                high = mid
            }
        }
        return (low + high) / 2.0
    }

    /* Проверка монотонности полинома */
    fun isMonotonic(steps: Int = 1000): Boolean {
        var prev = channelToEnergy(0)
        for (i in 1..steps) {
            val ch = i * maxChan / steps
            val curr = channelToEnergy(ch)
            if ((curr - prev) < -1e-8)
                return false // убывание — подозрительно
            prev = curr
        }
        return true
    }

    /* Перевод канала в энергию  (схема Горнера)
        E = pA * ch⁴ + pB * ch³ + pC * ch² + pD * ch + pE
    */
    public fun channelToEnergy(chan: Int): Float {
        val x = when (rS) {
            1 -> 2 * chan       // 2048 каналов
            2 -> chan           // 4096 каналов
            else -> 4 * chan    // 1024 канала
        }.toDouble()            // Double для точности

        // Схема Горнера: (((pA*x + pB)*x + pC)*x + pD)*x + pE
        val result = (((pA.toDouble() * x + pB.toDouble()) * x + pC.toDouble()) * x + pD.toDouble()) * x + pE.toDouble()

        return result.toFloat()
    }

    public fun channelToEnergyPolynom(chan: Int, ppA: Float, ppB: Float, ppC: Float, ppD: Float, ppE: Float): Float {
        val x = when (rS) {
            1 -> 2 * chan       // 2048 каналов
            2 -> chan           // 4096 каналов
            else -> 4 * chan    // 1024 канала
        }.toDouble()            // Double для точности

        // Схема Горнера: (((pA*x + pB)*x + pC)*x + pD)*x + pE
        val result = (((ppA.toDouble() * x + ppB.toDouble()) * x + ppC.toDouble()) * x + ppD.toDouble()) * x + ppE.toDouble()

        return result.toFloat()
    }

    /* Пересчет энергии в канал на основе коэффициентов полинома */
    public fun energyToChannelPolynom(energy: Float, ppA: Float, ppB: Float, ppC: Float, ppD: Float, ppE: Float) : Int {
        val target = energy.toDouble()
        val minCh = 0
        val maxCh = maxChan

        // Проверка границ
        val eMin = channelToEnergyPolynom(minCh, ppA, ppB, ppC, ppD, ppE).toDouble()
        val eMax = channelToEnergyPolynom(maxCh, ppA, ppB, ppC, ppD, ppE).toDouble()
        if (target < minOf(eMin, eMax) || target > maxOf(eMin, eMax)) {
            return -1  // вне диапазона
        }

        // Бинарный поиск
        var low = minCh
        var high = maxCh
        repeat(30) {  // 2^30 > 1e9, достаточно для любой разрядности
            val mid = (low + high) / 2
            val eMid = channelToEnergyPolynom(mid, ppA, ppB, ppC, ppD, ppE).toDouble()
            if (abs(eMid - target) < 0.01) return mid  // точность 0.01 кэВ
            if ((eMid < target) == (eMin < eMax)) {
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return (low + high) / 2
    }

    /* Перевод энергии в канал */
    public fun energyToChannel(energy: Float): Int {
        val target = energy.toDouble()
        val minCh = 0
        val maxCh = maxChan

        // Проверка границ
        val eMin = channelToEnergy(minCh).toDouble()
        val eMax = channelToEnergy(maxCh).toDouble()
        if (target < minOf(eMin, eMax) || target > maxOf(eMin, eMax)) {
            return -1  // вне диапазона
        }

        // Бинарный поиск
        var low = minCh
        var high = maxCh
        repeat(30) {  // 2^30 > 1e9, достаточно для любой разрядности
            val mid = (low + high) / 2
            val eMid = channelToEnergy(mid).toDouble()
            if (abs(eMid - target) < 0.01) return mid  // точность 0.01 кэВ
            if ((eMid < target) == (eMin < eMax)) {
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return (low + high) / 2
    }
}