package ru.starline.bluz

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.widget.ImageView
import java.lang.Math.log

/**
 * Created by ed on 17,февраль,2025
 */
class drawDozimeter {
    public var dozHSize: Int = 0
    public var dozVSize: Int = 0
    private lateinit var dozBitmap: Bitmap
    private lateinit var dozCanvas: Canvas
    public var dozimeterData: DoubleArray = DoubleArray(2048)
    public lateinit var dozView : ImageView
    public var dozxSize: Double = 4.0

    fun Init() {
        if (GO.drawDozObjectInit) {
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
        var dozPaint: Paint = Paint()
        var maxY: Double = 0.0
        var oldY: Double = 0.0
        var oldX: Double = 0.0
        var dozY: Float = 0.0f
        var dozX: Float = 0.0f
        var dozKoef: Double
        dozPaint.color = GO.ColorDosimeter        // Цвет для отображения статистики дозиметра
        dozPaint.strokeWidth = 2.0f

        for (iii: Int in 0 until 512) {
            if (maxY < dozimeterData[iii]) {
                maxY = dozimeterData[iii]
            }
        }

        dozKoef = dozVSize / maxY
        oldY = dozVSize - dozimeterData[0] * dozKoef
        var offsetX = 50.0f
        oldX = offsetX.toDouble() * dozxSize
        //Log.d("BluZ-BT", "Draw dozimeter. VSize $dozVSize")
        for (idx in 1 until 512) {
            if ( ! (oldY == dozVSize.toDouble() && dozimeterData[idx] == 0.0)) {
                dozY = (dozVSize - dozimeterData[idx] * dozKoef).toFloat()
                dozX = ((idx + offsetX) * dozxSize).toFloat()
                dozCanvas.drawLine(
                    oldX.toFloat(),       // Начальный X
                    oldY.toFloat(),       // Начальный Y
                    dozX,                 // Конечный X
                    dozY,                 // Конечный Y
                    dozPaint
                )
            }
            oldY = dozY.toDouble()
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