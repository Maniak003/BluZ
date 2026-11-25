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
import androidx.core.graphics.toColorInt

/**
 * Created by ed on 03,ноябрь,2026
 */
data class ColorStop(val position: Float, val color: Int)

class drawSGramm {
    var clr = IntArray(256)
    var hSize: Int = 0
    var vSize: Int = 0
    // Создаем битмап и канвас только когда размеры известны
    private lateinit var sGramCanvas: Canvas
    private lateinit var sGramBitmap: Bitmap

    fun interpolateColor(progress: Float, stops: List<ColorStop>): Int {
        // Ограничиваем прогресс от 0.0 до 1.0
        val clampedProgress = progress.coerceIn(0.0f, 1.0f)

        // Находим две ключевые точки, между которыми находится наш прогресс
        var lower = stops.first()
        var upper = stops.last()

        for (i in 0 until stops.size - 1) {
            if (clampedProgress >= stops[i].position && clampedProgress <= stops[i + 1].position) {
                lower = stops[i]
                upper = stops[i + 1]
                break
            }
        }

        // Если попали точно в точку или диапазон нулевой
        if (lower.position == upper.position) return lower.color

        // Вычисляем коэффициент смешивания (от 0.0 до 1.0) между нижней и верхней точкой
        val range = upper.position - lower.position
        val factor = (clampedProgress - lower.position) / range

        // Извлекаем RGB-компоненты обоих цветов
        val r1 = Color.red(lower.color)
        val g1 = Color.green(lower.color)
        val b1 = Color.blue(lower.color)

        val r2 = Color.red(upper.color)
        val g2 = Color.green(upper.color)
        val b2 = Color.blue(upper.color)

        // Линейно интерполируем каждый канал
        val r = (r1 + factor * (r2 - r1)).toInt()
        val g = (g1 + factor * (g2 - g1)).toInt()
        val b = (b1 + factor * (b2 - b1)).toInt()

        return Color.rgb(r, g, b)
    }

    // 3. Основная функция генерации палитры
    fun generateTemperaturePalette(size: Int): IntArray {
        val clr = IntArray(size)

        val stopsPhysics = listOf(
            ColorStop(0.0f, Color.BLACK),                       // Холодно
            ColorStop(0.3f, "#8B0000".toColorInt()),     // Темно-красный
            ColorStop(0.5f, Color.RED),                         // Красный
            ColorStop(0.7f, "#FFAA00".toColorInt()),     // Оранжевый
            ColorStop(0.85f, Color.YELLOW),                     // Желтый
            ColorStop(0.95f, Color.WHITE),                      // Белый
            ColorStop(1.0f, "#AAEEFF".toColorInt())      // Сверхгорячий (голубоватый)
        )


        // Задаем ключевые точки: позиция (от 0.0 до 1.0) и цвет
        // Вы можете менять позиции (0.2f, 0.4f и т.д.), чтобы растянуть или сжать участки цветов
        val stops = listOf(
            ColorStop(0.0f, Color.BLACK),                       // 0%: Абсолютный минимум (Черный)
            ColorStop(0.2f, "#0000FF".toColorInt()),     // 20%: Холодный (Синий)
            ColorStop(0.45f, Color.RED),                        // 45%: Теплый (Красный)
            ColorStop(0.65f, "#FF8800".toColorInt()),    // 65%: Горячий (Оранжевый)
            ColorStop(0.85f, Color.YELLOW),                     // 85%: Очень горячий (Желтый)
            ColorStop(1.0f, Color.WHITE)                        // 100%: Максимум (Белый)
        )

        for (i in 0 until size) {
            // Нормализуем индекс массива в диапазон от 0.0 до 1.0
            val progress = i.toFloat() / (size - 1)
            clr[i] = interpolateColor(progress, stops)
            //clr[i] = interpolateColor(progress, stopsPhysics)
        }

        return clr
    }

    /* Инициализация спектрограммы */
    fun init( hSz: Int, vSz: Int) {
        hSize = hSz
        vSize = vSz
        GO.drawSPECTER.sGrammIndex = 0

        for (i: Int in 1 until clr.size - 1) {
            val hue = i * 270f / clr.size               // От 0 до ~270 градусов
            val hsv = floatArrayOf(hue, 1.0f, 1.0f)     // Насыщенность и яркость = 100%
            clr[256 - i] = Color.HSVToColor(hsv)
        }
        clr[0] = 0

        //clr = generateTemperaturePalette(256)
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
        var maxSGramm: Double = 0.0
        var minSGramm: Double = 1000000.0
        for (row: Int in 0 until GO.drawSPECTER.rowsSGramm)  {
            for (col: Int in 0 until GO.drawSPECTER.colsSGramm) {
                /* Test */
                //sGrammBuffer[row][col] = col.toUInt()

                if (maxSGramm < GO.drawSPECTER.sGrammBuffer[row][col]) {
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
            strokeWidth = GO.drawSPECTER.colsSGramm.toFloat() / sGramCanvas.width
            isAntiAlias = false
        }
        val kfHSize = (hSize.toFloat() / GO.drawSPECTER.colsSGramm)
        val kfVSize = (vSize.toFloat() / GO.drawSPECTER.rowsSGramm)
        Log.i("BluZ-BT", "Draw spectrogramm: ${hSize}x${vSize}, index: ${GO.drawSPECTER.sGrammIndex}")
        //Log.d("BluZ-BT", "Spectrogramm HSize: $hSize, VSize: $vSize, kfHSize: $kfHSize, kfVSize: $kfVSize, rowsSGramm: ${GO.drawSPECTER.rowsSGramm}")
        for (row: Int in 0 until GO.drawSPECTER.rowsSGramm ) {
            val y: Float = kfVSize * row
            for (col: Int in 0 until GO.drawSPECTER.colsSGramm) {
                val x: Float =  kfHSize * col
                val pntCol = (GO.drawSPECTER.sGrammBuffer[row][col] - minSGramm).toFloat() * kfColor
                val colorIndex = pntCol.toInt().coerceIn(0, clr.size - 1)
                paintPixel.color = clr[colorIndex]
                sGramCanvas.drawPoint(x, y * paintPixel.strokeWidth , paintPixel)
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
                GO.drawSPECTER.sGrammBuffer[row][col] = 0.0
            }
        }
    }
}