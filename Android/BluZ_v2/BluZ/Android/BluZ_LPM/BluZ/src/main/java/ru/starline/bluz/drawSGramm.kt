package ru.starline.bluz

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.DrawableCompat
import kotlin.random.Random
import androidx.core.graphics.createBitmap

/**
 * Created by ed on 03,ноябрь,2026
 */
class drawSGramm {
    var clr = IntArray(256)
    var hSize: Int = 0
    var vSize: Int = 0
    // Создаем битмап и канвас только когда размеры известны
    private lateinit var sGramCanvas: Canvas
    private lateinit var sGramBitmap: Bitmap


    /* Инициализация спектрограммы */
    fun init( hSz: Int, vSz: Int) {
        hSize = hSz
        vSize = vSz
        clr[0] = 0
        GO.drawSPECTER.sGrammIndex = 0
        for (i: Int in 1 until clr.size - 1) {
            val hue = i * 270f / clr.size  // От 0 до ~270 градусов
            val hsv = floatArrayOf(hue, 1.0f, 1.0f)  // Насыщенность и яркость = 100%
            clr[i] = Color.HSVToColor(hsv)
        }
        GO.drawSGinit = true
        sGramBitmap = createBitmap(hSize, vSize)
        sGramCanvas = Canvas(sGramBitmap)
        GO.sGramView.setImageBitmap(sGramBitmap)
    }

    /* Перерисовка спектрограммы */
    @RequiresApi(Build.VERSION_CODES.Q)
    @OptIn(ExperimentalUnsignedTypes::class)
    fun redraw() {
        /* Поиск максимально и минимального значений, для раскраски */
        var maxSGramm: UInt = 0u
        var minSGramm: UInt = 0u - 1u
        for (row: Int in 0 until GO.drawSPECTER.rowsSGramm )  {
            for (col: Int in 0 until GO.drawSPECTER.colsSGramm) {
                /* Test */
                //sGrammBuffer[row][col] = col.toUInt()

                if (maxSGramm < GO.drawSPECTER.sGrammBuffer[row][col] ) {
                    maxSGramm = GO.drawSPECTER.sGrammBuffer[row][col]
                }
                if (minSGramm > GO.drawSPECTER.sGrammBuffer[row][col]) {
                    minSGramm = GO.drawSPECTER.sGrammBuffer[row][col]
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
        val kfHSize = (hSize.toFloat() / GO.drawSPECTER.colsSGramm)
        val kfVSize = (vSize.toFloat() / GO.drawSPECTER.rowsSGramm)
        var virtRow = GO.drawSPECTER.sGrammIndex
        Log.i("BluZ-BT", "Draw spectrogramm")
        //Log.d("BluZ-BT", "Spectrogramm HSize: $hSize, VSize: $vSize, kfHSize: $kfHSize, kfVSize: $kfVSize, rowsSGramm: ${GO.drawSPECTER.rowsSGramm}")
        for (row: Int in 0 until GO.drawSPECTER.rowsSGramm )  {
            val y: Float = kfVSize * row
            for (col: Int in 0 until GO.drawSPECTER.colsSGramm) {
                val x: Float =  kfHSize * col
                val pntCol = (GO.drawSPECTER.sGrammBuffer[row][col] - minSGramm).toFloat() * kfColor
                paintPixel.color = clr[(clr.size - 1) - pntCol.toInt()]
                sGramCanvas.drawPoint(x, y, paintPixel)
            }
            //virtRow--
            //if (virtRow <= 0) {
            //    virtRow = GO.drawSPECTER.rowsSGramm - 1
            //}
        }
    }

    /* Очистка спектрограммы */
    @OptIn(ExperimentalUnsignedTypes::class)
    fun clear() {
        /* Очистим буфер спектрограммы */
        for (row: Int in 0 until GO.drawSPECTER.rowsSGramm )  {
            for (col: Int in 0  until GO.drawSPECTER.colsSGramm) {
                GO.drawSPECTER.sGrammBuffer[row][col] = 0u
            }
        }
    }
}