package ru.starline.bluz

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import java.lang.Math.log

/**
 * Created by ed on 17,февраль,2025
 */
class drawDozimeter {
    public var dozHSize: Int = 0
    public var dozVSize: Int = 0
    public lateinit var textMax: TextView
    public lateinit var textMin: TextView
    private val dozimeterSize = 512
    private lateinit var dozBitmap: Bitmap
    private lateinit var dozCanvas: Canvas
    public var dozimeterData: DoubleArray = DoubleArray(dozimeterSize)
    public var dozimeterSMA: DoubleArray = DoubleArray(dozimeterSize)
    public lateinit var dozView : ImageView
    public var dozxSize: Double = 4.0

    fun Init() {
        if (GO.drawDozObjectInit) {
            if (!this::dozView.isInitialized) {
                Log.w("BluZ-BT", "dozView not initialized, call init() first")
                return
            }
            dozHSize = dozView.width
            dozVSize = dozView.height
            if ((dozHSize == 0) or (dozVSize == 0)) {
                Log.e("BluZ-BT", "HSize: $dozHSize, VSize: $dozVSize")
            } else {
                /* Подготавливаем bitmap для рисования */
                dozBitmap = Bitmap.createBitmap(dozHSize, dozVSize, Bitmap.Config.ARGB_8888)
                dozCanvas = Canvas(dozBitmap)
                GO.drawDozObjectInit = false
            }
        } else {
            dozView.setImageBitmap(dozBitmap)
        }
    }

    /*
    *   Прорисовка данных дозиметра
    */
    fun redrawDozimeter() {
        if (!this::dozBitmap.isInitialized) {
            Log.w("BluZ-BT", "dozBitmap not initialized, call init() first")
            return
        }
        if (!this::dozView.isInitialized) {
            Log.w("BluZ-BT", "dozView not initialized, call init() first")
            return
        }
        var offsetX = 50.0f
        var maxY: Double = 0.0
        var minY: Double = 65536.0
        var maxYSMA: Double = 0.0
        var minYSMA: Double = 65536.0
        var oldY: Double = 0.0
        var oldYSMA: Double = 0.0
        var oldX: Double = offsetX.toDouble()
        var dozY: Float = 0.0f
        var dozYSMA: Float = 0.0f
        var dozX: Float = 0.0f
        var dozKoef: Double
        var SMAWindow: Int = 20
        var dozPaint: Paint = Paint()
        dozPaint.color = GO.ColorDosimeter        // Цвет для отображения статистики дозиметра
        dozPaint.strokeWidth = 2.0f
        var dozPaintSMA: Paint = Paint()
        dozPaintSMA.color = GO.ColorDosimeterSMA        // Цвет для отображения статистики дозиметра
        dozPaintSMA.strokeWidth = 4.0f
        var mIdx: Int = 0
        for (iii: Int in 0 until dozimeterSize) {
            if (maxY < dozimeterData[iii]) {
                maxY = dozimeterData[iii]
            }
            if (minY > dozimeterData[iii]) {
                minY = dozimeterData[iii]
                mIdx = iii
            }
        }

        var tmpSM: Double
        for (ttt: Int in 0 until dozimeterSize - SMAWindow) {
            tmpSM = 0.0
            for (k in 0 until SMAWindow) {
                tmpSM += dozimeterData[ttt + k]
            }
            dozimeterSMA[ttt] = tmpSM / SMAWindow
        }

        textMax.text = "cps:${maxY.toInt()}"
        textMin.text = "cps:${minY.toInt()}"

        var deltaY = maxY - minY
        if (deltaY > 0) {
            dozKoef = dozVSize / deltaY
        } else {
            if (maxY > 0) {
                dozKoef = dozVSize / maxY / 2       // Если нет измененний, выведем линию по середине экрана.
            } else {
                dozKoef = 1.0
            }
        }
        oldY = dozVSize - (dozimeterData[0] - minY) * dozKoef
        oldYSMA = dozVSize - (dozimeterSMA[0] - minY) * dozKoef
        oldX = offsetX.toDouble()
        dozxSize = ((dozHSize - offsetX) / dozimeterSize).toDouble()
        Log.d("BluZ-BT", "Draw dozimeter. dozxSize: $dozxSize  VSize $dozVSize, MAX: $maxY, MIN[$mIdx]: $minY")
        for (idx in 0 until dozimeterSize) {
            dozY = (dozVSize - (dozimeterData[idx] - minY) * dozKoef).toFloat()
            dozX = ((idx) * dozxSize  + offsetX).toFloat()
            dozCanvas.drawLine(
                oldX.toFloat(),       // Начальный X
                oldY.toFloat(),       // Начальный Y
                dozX,                 // Конечный X
                dozY,                 // Конечный Y
                dozPaint
            )

            if (idx < dozimeterSize - SMAWindow) {
                dozYSMA = (dozVSize - (dozimeterSMA[idx] - minY) * dozKoef).toFloat()
                dozCanvas.drawLine(
                    oldX.toFloat(),       // Начальный X
                    oldYSMA.toFloat(),    // Начальный Y
                    dozX,                 // Конечный X
                    dozYSMA,              // Конечный Y
                    dozPaintSMA
                )
            }
            oldY = dozY.toDouble()
            oldYSMA = dozYSMA.toDouble()
            oldX = dozX.toDouble()
        }
        dozView.setImageBitmap(dozBitmap)
    }
    fun clearDozimeter() {
        //Log.d("BluZ-BT", "Clear dozimeter.")
        if ((dozHSize > 0) and (dozVSize > 0)) {
            //specBitmap = Bitmap.createBitmap(HSize, VSize, Bitmap.Config.ARGB_8888)
            //specCanvas = Canvas(specBitmap)
            dozCanvas.drawColor(Color.argb(255, 0, 0, 0))
            dozView.setImageBitmap(dozBitmap)
        } else {
            Log.e("BluZ-BT", "HSize: $dozHSize, VSize: $dozVSize")
        }
    }
}