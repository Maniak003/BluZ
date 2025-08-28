package ru.starline.bluz

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import kotlin.math.ln

class drawHistory {
    public var HSize: Int = 0
    public var VSize: Int = 0
    private lateinit var histBitmap: Bitmap
    private lateinit var histCanvas: Canvas
    public lateinit var imgView : ImageView
    public var historyData: DoubleArray = DoubleArray(4096)
    public var tmpHistoryData: DoubleArray = DoubleArray(4096)
    public var flagSMA: Boolean = false
    public var flagMEDIAN: Boolean = false
    public var ResolutionHistory: Int = 1024;
    public var koefLog: Double = 1.0
    public var koefLin: Double = 1.0
    public var xSize: Double = 1.0
    private var MF: DoubleArray = DoubleArray(3)
    private var indexMF: Int = 0

    /* Установка рабочих параметров и создание необходимых объектов */
    fun init() {
        if (GO.drawObjectInitHistory) {
            HSize = imgView.width
            VSize = imgView.height
            if ((HSize == 0) or (VSize == 0)) {
                Log.e("BluZ-BT", "HSize: $HSize, VSize: $VSize, Res: $ResolutionHistory")
            } else {
                /* Подготавливаем bitmap для рисования */
                histBitmap = createBitmap(HSize, VSize)
                histCanvas = Canvas(histBitmap)
                GO.drawObjectInitHistory = false
            }
        } else {
            imgView.setImageBitmap(histBitmap)
        }
        //txtStat1.text = saveStat1
        //txtStat2.text = saveStat2
        //txtStat3.text = saveStat3
    }
    fun redrawSpecter(spType: Int) {
        Log.d("BluZ-BT", "Type: $spType HSize: $HSize VSize: $VSize, Res: $ResolutionHistory")
        //Log.d("BluZ-BT", "Draw specter.")
        var paintLin: Paint = Paint()
        var paintLog: Paint = Paint()

        /*
         *  Подготовка массива для отрисовки данных
         *  Функии фильтрации
         */

        var tmpSM: Double
        for (idx: Int in 0 until 3) {
            MF[idx] = 0.0
        }
        indexMF = 0
        for (ttt: Int in 0 until ResolutionHistory - GO.windowSMA) {
            if (ttt > GO.rejectChann) {
                /* SMA фильтр */
                if (flagSMA) {
                    /* SMA */
                    tmpSM = 0.0
                    for (k in 0 until GO.windowSMA) {
                        tmpSM += historyData[ttt + k]
                    }
                    tmpHistoryData[ttt] = tmpSM / GO.windowSMA
                } else {
                    /* Без преобразования */
                    tmpHistoryData[ttt] = historyData[ttt]
                }
                /* Медианный фильтр */
                if (flagMEDIAN) {
                    MF[indexMF] = tmpHistoryData[ttt]
                    tmpHistoryData[ttt] = MF.sorted()[1]
                    if (++indexMF >= 3) {
                        indexMF = 0
                    }
                }
            } else {
                tmpHistoryData[ttt] = 0.0       // Удалим данные из не используемых каналов
            }
        }

        xSize = HSize.toDouble() / ResolutionHistory
        paintLin.strokeWidth = xSize.toFloat()
        paintLog.strokeWidth = xSize.toFloat()

        if (GO.specterGraphType == 0) {         // Линейный стиль графика
            paintLin.color = GO.ColorLin        // Цвет для отображения линейного спектра
            paintLog.color = GO.ColorLog        // Цвет для отображения логарифмического спектра
        } else {                                // Стиль гистограмма
            paintLin.color = GO.ColorLinGisto   // Цвет для отображения линейного спектра
            paintLog.color = GO.ColorLogGisto   // Цвет для отображения логарифмического спектра
        }
        var oldYlin: Double = VSize.toDouble()
        var oldYlog: Double = VSize.toDouble()
        var oldX: Double = 0.0
        var maxYlin: Double = 0.0
        var maxYlog: Double = 0.0
        var tmpLog: Double

        /* Поиск максимального значения для масштабирования */
        for (idx in GO.rejectChann .. ResolutionHistory - 1) {
            if (maxYlin < tmpHistoryData[idx]) {
                maxYlin = tmpHistoryData[idx]
            }
        }
        maxYlog = ln(maxYlin);
        //Log.d("BluZ-BT", "MAX: : $maxYlin")
        koefLin = VSize / maxYlin
        koefLog = VSize / maxYlog
        var Ylin: Float
        var Ylog: Float
        for (idx in 0 until ResolutionHistory) {
            Ylin = (VSize - tmpHistoryData[idx] * koefLin).toFloat()
            if (tmpHistoryData[idx] != 0.0) {
                Ylog = (VSize - ln(tmpHistoryData[idx]) * koefLog).toFloat()
            } else {
                Ylog = VSize.toFloat()
            }
            if (GO.specterGraphType == 0) {         // Стиль графика - линия
                /* Прорисовка линейного графика */

                if ( ! (oldYlin == VSize.toDouble() && tmpHistoryData[idx] == 0.0)) {
                    histCanvas.drawLine(
                        (oldX * xSize).toFloat(),   // Начальный X
                        oldYlin.toFloat(),          // Начальный Y
                        (idx * xSize).toFloat(),    // Конечный X
                        Ylin,                       // Конечный Y
                        paintLin
                    )
                }
                /* Прорисовка логарифмического графика */
                if ( ! (oldYlog == VSize.toDouble() /*&& spectrData[idx] == 0.0*/ && Ylog == VSize.toFloat())) {
                    histCanvas.drawLine(
                        (oldX * xSize).toFloat(),   // Начальный X
                        oldYlog.toFloat(),          // Начальный Y
                        (idx * xSize).toFloat(),    // Конечный X
                        Ylog,                       // Конечный Y
                        paintLog
                    )
                }
            } else {                                // Стиль графика - гистограмма
                /* Линейный график */
                histCanvas.drawLine((idx * xSize).toFloat(), Ylin, (idx * xSize).toFloat(),VSize.toFloat(), paintLin )
                /* Логарифмический график */
                histCanvas.drawLine((idx * xSize).toFloat(), Ylog, (idx * xSize).toFloat(),VSize.toFloat(), paintLog )
            }
            oldYlin = Ylin.toDouble()
            oldYlog = Ylog.toDouble()
            oldX = idx.toDouble()
        }
        imgView.setImageBitmap(histBitmap)
    }

    fun clearHistory() {
        //Log.d("BluZ-BT", "Clear specter.")
        if ((HSize > 0) and (VSize > 0)) {
            //specBitmap = Bitmap.createBitmap(HSize, VSize, Bitmap.Config.ARGB_8888)
            //specCanvas = Canvas(specBitmap)
            histCanvas.drawColor(Color.argb(255, 0, 0, 0))
            imgView.setImageBitmap(histBitmap)
        } else {
            Log.e("BluZ-BT", "HSize: $HSize, VSize: $VSize")
        }
    }

}