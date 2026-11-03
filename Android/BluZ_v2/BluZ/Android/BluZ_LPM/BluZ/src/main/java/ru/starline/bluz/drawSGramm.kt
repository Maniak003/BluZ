package ru.starline.bluz

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.DrawableCompat
import kotlin.random.Random

/**
 * Created by ed on 03,ноябрь,2026
 */
class drawSGramm {
    @OptIn(ExperimentalUnsignedTypes::class)

    public var sGrammBuffer = Array(4096) { UIntArray(1024) }
    @OptIn(ExperimentalUnsignedTypes::class)
    val rowsSGramm = sGrammBuffer.size
    @OptIn(ExperimentalUnsignedTypes::class)
    val colsSGramm = sGrammBuffer[0].size

    var clr = IntArray(256)

    /* Инициализация спектрограммы */
    fun init() {
        for (i: Int in 0 until clr.size) {
            val hue = i * 270f / clr.size  // От 0 до ~270 градусов
            val hsv = floatArrayOf(hue, 1.0f, 1.0f)  // Насыщенность и яркость = 100%
            clr[i] = Color.HSVToColor(hsv)
        }
    }

    /* Перерисовка спектрограммы */
    @RequiresApi(Build.VERSION_CODES.Q)
    @OptIn(ExperimentalUnsignedTypes::class)
    fun redraw(sGramCanvas: Canvas) {
        /* Поиск максимально и минимального значений, для раскраски */
        var maxSGramm: UInt = 0u
        var minSGramm: UInt = 0u - 1u
        for (row: Int in 0 until rowsSGramm )  {

            for (col: Int in 0 until colsSGramm) {
                /* Test */
                sGrammBuffer[row][col] = col.toUInt()

                if (maxSGramm < sGrammBuffer[row][col] ) {
                    maxSGramm = sGrammBuffer[row][col]
                }
                if (minSGramm > sGrammBuffer[row][col]) {
                    minSGramm = sGrammBuffer[row][col]
                }
            }
        }

        /* Расчет коэффициента для раскраски спектрограммы */
        val kfColor: Float = (clr.size - 1) / (maxSGramm - minSGramm).toFloat()

        /* Прорисовка данных из массива на канвасе */
        val hSize = sGramCanvas.width
        val vSize = sGramCanvas.height
        val paintPixel = Paint().apply {
            strokeWidth = 3.0f
            isAntiAlias = false
        }
        val kfHSize = (hSize.toFloat() / colsSGramm)
        val kfVSize = (vSize.toFloat() / rowsSGramm)
        Log.d("BluZ-BT", "Spectrogramm HSize: $hSize, VSize: $vSize, kfHSize: $kfHSize, kfVSize: $kfVSize, rowsSGramm: $rowsSGramm")
        for (row: Int in 0 until rowsSGramm )  {
            val y: Float = kfVSize * row
            for (col: Int in 0 until colsSGramm) {
                val x: Float =  kfHSize * col
                val pntCol = (sGrammBuffer[row][col] - minSGramm).toFloat() * kfColor
                paintPixel.color = clr[(clr.size - 1) - pntCol.toInt()]
                sGramCanvas.drawPoint(x, y, paintPixel)
            }
        }
    }

    /* Очистка спектрограммы */
    @OptIn(ExperimentalUnsignedTypes::class)
    fun clear() {
        /* Очистим буфер спектрограммы */
        for (row: Int in 0 until rowsSGramm )  {
            for (col: Int in 0  until colsSGramm) {
                sGrammBuffer[row][col] = 0u
            }
        }
    }
}