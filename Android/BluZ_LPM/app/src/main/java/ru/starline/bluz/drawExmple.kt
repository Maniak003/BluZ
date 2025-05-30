package ru.starline.bluz

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.widget.ImageView
import java.lang.Math.log

/**
 * Created by ed on 30,май,2025
 */
class drawExmple {
    public var HSize: Int = 0
    public var VSize: Int = 0
    private lateinit var exampleBitmap: Bitmap
    private lateinit var exampleCanvas: Canvas
    public lateinit var exampleImgView : ImageView
    public var exampleData: DoubleArray = DoubleArray(200)
    public var exampleFoneData: DoubleArray = DoubleArray(200)
    public var paintLin: Paint = Paint()
    public var paintLog: Paint = Paint()
    public var paintFoneLin: Paint = Paint()
    public var paintFoneLog: Paint = Paint()
    private var scaleCoefX: Double = 1.0
    private var scaleCoefLinY: Double = 1.0
    private var scaleCoefLogY: Double = 1.0

    /* Установка рабочих параметров и создание необходимых объектов */
    fun init() {
        if (GO.exampleObjectInit) {
            HSize = exampleImgView.width
            VSize = exampleImgView.height
            if ((HSize == 0) or (VSize == 0)) {
                Log.e("BluZ-BT", "HSize: $HSize, VSize: $VSize")
            } else {
                /* Подготавливаем bitmap для рисования */
                exampleBitmap = Bitmap.createBitmap(HSize, VSize, Bitmap.Config.ARGB_8888)
                exampleCanvas = Canvas(exampleBitmap)
                GO.exampleObjectInit = false
            }
        } else {
            exampleImgView.setImageBitmap(exampleBitmap)
        }
        for (tt: Int in 0 until exampleData.size) {
            exampleData[tt] = tt.toDouble()
        }
        for (tt: Int in 50 until exampleData.size) {
            exampleFoneData[tt] = tt.toDouble() - 50.0
        }
    }

    /* Прорисовка примера */
    fun exampRedraw() {
        init()
        if ( GO.exampleObjectInit) {
            return
        }
        clearExample()
        /* Выбор цветов для графиков */
        if (GO.specterGraphType == 0) {         // Стиль графика - линия
            paintLin.color = GO.ColorLin
            paintLog.color = GO.ColorLog
            paintFoneLin.color = GO.ColorFone
            paintFoneLog.color = GO.ColorFoneLg
        } else {                                // Стиль графика - гистограмма
            paintLin.color = GO.ColorLinGisto
            paintLog.color = GO.ColorLogGisto
            paintFoneLin.color = GO.ColorFoneGisto
            paintFoneLog.color = GO.ColorFoneLgGisto
        }
        scaleCoefX = HSize / exampleData.size.toDouble()
        scaleCoefLinY = VSize / exampleData.size.toDouble()
        scaleCoefLogY = VSize / log(exampleData.size.toDouble())
        var oldYlin: Double = VSize.toDouble()
        var oldYlog: Double = VSize.toDouble()
        var oldYlinFone: Double = VSize.toDouble()
        var oldYlogFone: Double = VSize.toDouble()
        var oldX: Double = 0.0
        var Ylin: Float
        var Ylog: Float
        var YlinFone: Float
        var YlogFone: Float
        for (idx: Int in 0 until exampleData.size) {
            Ylin = (VSize - exampleData[idx] * scaleCoefLinY).toFloat()
            YlinFone = (VSize - exampleFoneData[idx] * scaleCoefLinY).toFloat()
            if (exampleData[idx] != 0.0) {
                Ylog = (VSize - log(exampleData[idx]) * scaleCoefLogY).toFloat()
            } else {
                Ylog = VSize.toFloat()
            }
            if (exampleFoneData[idx] != 0.0) {
                YlogFone = (VSize - log(exampleFoneData[idx]) * scaleCoefLogY).toFloat()
            } else {
                YlogFone = VSize.toFloat()
            }
            if (GO.specterGraphType == 0) {         // Стиль графика - линия
                /* Прорисовка линейного графика */
                if ( ! (oldYlin == VSize.toDouble() && exampleData[idx] == 0.0)) {
                    exampleCanvas.drawLine(
                        (oldX * scaleCoefX).toFloat(),  // Начальный X
                        oldYlin.toFloat(),              // Начальный Y
                        (idx * scaleCoefX).toFloat(),   // Конечный X
                        Ylin,                           // Конечный Y
                        paintLin
                    )
                }

                /* Прорисовка линейного графика фона */
                if ( ! (oldYlinFone == VSize.toDouble() && exampleFoneData[idx] == 0.0)) {
                    exampleCanvas.drawLine(
                        (oldX * scaleCoefX).toFloat(),   // Начальный X
                        oldYlinFone.toFloat(),           // Начальный Y
                        (idx * scaleCoefX).toFloat(),    // Конечный X
                        YlinFone,                        // Конечный Y
                        paintFoneLin
                    )
                }
                /* Прорисовка логарифмического графика */
                if ( ! (oldYlog == VSize.toDouble() /*&& spectrData[idx] == 0.0*/ && Ylog == VSize.toFloat())) {
                    exampleCanvas.drawLine(
                        (oldX * scaleCoefX).toFloat(),      // Начальный X
                        oldYlog.toFloat(),                  // Начальный Y
                        (idx * scaleCoefX).toFloat(),       // Конечный X
                        Ylog,                               // Конечный Y
                        paintLog
                    )
                }
                /* Прорисовка логарифмического графика фона */
                if ( ! (oldYlogFone == VSize.toDouble() /*&& spectrData[idx] == 0.0*/ && YlogFone == VSize.toFloat())) {
                    exampleCanvas.drawLine(
                        (oldX * scaleCoefX).toFloat(),      // Начальный X
                        oldYlogFone.toFloat(),              // Начальный Y
                        (idx * scaleCoefX).toFloat(),       // Конечный X
                        YlogFone,                           // Конечный Y
                        paintFoneLog
                    )
                }
            } else {                                // Стиль графика - гистограмма
                /* Линейный график */
                exampleCanvas.drawLine((idx * scaleCoefX).toFloat(), Ylin, (idx * scaleCoefX).toFloat(),VSize.toFloat(), paintLin )
                /* Логарифмический график */
                exampleCanvas.drawLine((idx * scaleCoefX).toFloat(), Ylog, (idx * scaleCoefX).toFloat(),VSize.toFloat(), paintLog )
                /* Линейный график фона */
                exampleCanvas.drawLine((idx * scaleCoefX).toFloat(), YlinFone, (idx * scaleCoefX).toFloat(),VSize.toFloat(), paintFoneLin )
                /* Логарифмический график фона */
                exampleCanvas.drawLine((idx * scaleCoefX).toFloat(), YlogFone, (idx * scaleCoefX).toFloat(),VSize.toFloat(), paintFoneLog )
            }
            oldYlin = Ylin.toDouble()
            oldYlog = Ylog.toDouble()
            oldYlinFone = YlinFone.toDouble()
            oldYlogFone = YlogFone.toDouble()
            oldX = idx.toDouble()
            exampleImgView.setImageBitmap(exampleBitmap)
        }
    }
    fun clearExample() {
        //Log.d("BluZ-BT", "Clear specter.")
        if ((HSize > 0) and (VSize > 0)) {
            //specBitmap = Bitmap.createBitmap(HSize, VSize, Bitmap.Config.ARGB_8888)
            //specCanvas = Canvas(specBitmap)
            exampleCanvas.drawColor(Color.argb(255, 0, 0, 0))
            exampleImgView.setImageBitmap(exampleBitmap)
        } else {
            Log.e("BluZ-BT", "HSize: $HSize, VSize: $VSize")
        }
    }
}