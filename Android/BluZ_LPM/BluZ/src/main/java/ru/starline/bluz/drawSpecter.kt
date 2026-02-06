package ru.starline.bluz

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Html
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized
import ru.starline.bluz.MainActivity
import java.lang.Math.log
import java.lang.Math.round
import java.lang.Math.sqrt
import java.util.Locale
import kotlin.math.ln
import androidx.core.graphics.createBitmap


/**
 * Created by ed on 21,июнь,2024
 */
class drawSpecter {
    public var HSize: Int = 0
    public var VSize: Int = 0
    private lateinit var specBitmap: Bitmap
    private lateinit var specCanvas: Canvas
    public lateinit var imgView : ImageView
    public var mlemBuffer = DoubleArray(4096)
    public var spectrData: DoubleArray = DoubleArray(4096)
    public var tmpSpecterData: DoubleArray = DoubleArray(4096)
    public var flagSMA: Boolean = false
    public var flagMEDIAN: Boolean = false
    public var flagMLEM: Boolean = false
    public var ResolutionSpectr: Int = 1024;
    public var koefLog: Double = 1.0
    public var koefLin: Double = 1.0
    public var koefLinMLEM: Double = 1.0
    public var koefLogMLEM: Double = 1.0
    public var xSize: Double = 1.0
    private var MF: DoubleArray = DoubleArray(3)
    private var indexMF: Int = 0

    /* Установка рабочих параметров и создание необходимых объектов */
    fun init() {
        if (! this::imgView.isInitialized) {
            Log.e("BluZ-BT", "imgView not init.")
            return
        }
        if (GO.drawObjectInit or !this::specBitmap.isInitialized) {
            HSize = imgView.width
            VSize = imgView.height
            if ((HSize == 0) or (VSize == 0)) {
                Log.e("BluZ-BT", "HSize: $HSize, VSize: $VSize, Res: $ResolutionSpectr")
            } else {
                /* Подготавливаем bitmap для рисования */
                specBitmap = createBitmap(HSize, VSize)
                specCanvas = Canvas(specBitmap)
                GO.drawObjectInit = false
            }
        } else {
            imgView.setImageBitmap(specBitmap)
        }
        //txtStat1.text = saveStat1
        //txtStat2.text = saveStat2
        //txtStat3.text = saveStat3
    }

    fun redrawSpecter(spType: Int) {
        if (!this::specBitmap.isInitialized) {
            Log.w("BluZ-BT", "specBitmap not initialized, call init() first")
            return
        }
        Log.d("BluZ-BT", "Type: $spType HSize: $HSize VSize: $VSize, Res: $ResolutionSpectr")
        //Log.d("BluZ-BT", "Draw specter.")
        var paintLin: Paint = Paint()
        var paintLog: Paint = Paint()
        var paintFoneLin = Paint()
        var paintFoneLog = Paint()

        /*
         *  Подготовка массива для отрисовки данных
         *  Функии фильтрации
         */

        var tmpSM: Double
        for (idx: Int in 0 until 3) {
            MF[idx] = 0.0
        }
        indexMF = 0
        for (ttt: Int in 0 until ResolutionSpectr - GO.windowSMA) {
            if (ttt > GO.rejectChann) {
                /* SMA фильтр */
                if (flagSMA) {
                    /* SMA */
                    tmpSM = 0.0
                    for (k in 0 until GO.windowSMA) {
                        tmpSM += spectrData[ttt + k]
                    }
                    tmpSpecterData[ttt] = tmpSM / GO.windowSMA
                } else {
                    /* Без преобразования */
                    tmpSpecterData[ttt] = spectrData[ttt]
                }
                /* Медианный фильтр */
                if (flagMEDIAN) {
                    MF[indexMF] = tmpSpecterData[ttt]
                    tmpSpecterData[ttt] = MF.sorted()[1]
                    if (++indexMF >= 3) {
                        indexMF = 0
                    }
                }
            } else {
                tmpSpecterData[ttt] = 0.0       // Удалим данные из не используемых каналов
            }
        }

        xSize = HSize.toDouble() / ResolutionSpectr
        paintLin.strokeWidth = xSize.toFloat()
        paintLog.strokeWidth = xSize.toFloat()
        paintFoneLin.strokeWidth = xSize.toFloat() * 2
        paintFoneLog.strokeWidth = xSize.toFloat() * 2

        if (GO.specterGraphType == 0) {         // Линейный стиль графика
            paintLin.color = GO.ColorLin        // Цвет для отображения линейного спектра
            paintLog.color = GO.ColorLog        // Цвет для отображения логарифмического спектра
            paintFoneLin.color = GO.ColorFone
            paintFoneLog.color = GO.ColorFoneLg
        } else {                                // Стиль гистограмма
            paintLin.color = GO.ColorLinGisto   // Цвет для отображения линейного спектра
            paintLog.color = GO.ColorLogGisto   // Цвет для отображения логарифмического спектра
            paintFoneLin.color = GO.ColorFoneGisto
            paintFoneLog.color = GO.ColorFoneLgGisto
        }
        var oldYlin: Double = VSize.toDouble()
        var oldYlog: Double = VSize.toDouble()
        var oldMLEMLin: Double = VSize.toDouble()
        var oldMLEMLog: Double = VSize.toDouble()
        var oldX: Double = 0.0
        var oldXMLEM: Double = 0.0
        var maxYlin: Double = 0.0
        var maxYlog: Double = 0.0
        //var tmpLog: Double
        var maxMLEMLin: Double = 0.0
        var maxMLENLog: Double = 0.0

        /* Поиск максимального значения массива MLEM */
        if (flagMLEM) { // Массив готов и можно прорисовывать
            for(idxm in 20 until ResolutionSpectr) {
                if (maxMLEMLin < mlemBuffer[idxm]) {
                    maxMLEMLin = mlemBuffer[idxm]
                }
            }
            maxMLENLog = ln(maxMLEMLin)
        }
        /* Поиск максимального значения для масштабирования */
        for (idx in GO.rejectChann .. ResolutionSpectr - 1) {
            if (maxYlin < tmpSpecterData[idx]) {
                maxYlin = tmpSpecterData[idx]
            }
        }
        maxYlog = ln(maxYlin);
        //Log.d("BluZ-BT", "MAX: : $maxYlin")
        koefLin = VSize / maxYlin
        koefLog = VSize / maxYlog
        koefLinMLEM = VSize / maxMLEMLin
        koefLogMLEM = VSize / maxMLENLog
        var YLinMLEM: Float = 0f
        var YLogMLEM: Float = 0f
        var Ylin: Float
        var Ylog: Float
        for (idx in 0 until ResolutionSpectr) {
            /* Подготовка данных спектра */
            Ylin = (VSize - tmpSpecterData[idx] * koefLin).toFloat()
            if (tmpSpecterData[idx] != 0.0) {
                Ylog = (VSize - ln(tmpSpecterData[idx]) * koefLog).toFloat()
            } else {
                Ylog = VSize.toFloat()
            }
            if (GO.specterGraphType == 0) {         // Стиль графика - линия
                /* Прорисовка линейного графика */

                if ( ! (oldYlin == VSize.toDouble() || tmpSpecterData[idx] == 0.0)) {
                    specCanvas.drawLine(
                        (oldX * xSize).toFloat(),   // Начальный X
                        oldYlin.toFloat(),          // Начальный Y
                        (idx * xSize).toFloat(),    // Конечный X
                        Ylin,                       // Конечный Y
                        paintLin
                    )
                }
                /* Прорисовка логарифмического графика */
                if ( ! (oldYlog == VSize.toDouble() /*&& spectrData[idx] == 0.0*/ || Ylog == VSize.toFloat())) {
                    specCanvas.drawLine(
                        (oldX * xSize).toFloat(),   // Начальный X
                        oldYlog.toFloat(),          // Начальный Y
                        (idx * xSize).toFloat(),    // Конечный X
                        Ylog,                       // Конечный Y
                        paintLog
                    )
                }
            } else {                                // Стиль графика - гистограмма
                /* Линейный график */
                specCanvas.drawLine((idx * xSize).toFloat(), Ylin, (idx * xSize).toFloat(),VSize.toFloat(), paintLin )
                /* Логарифмический график */
                specCanvas.drawLine((idx * xSize).toFloat(), Ylog, (idx * xSize).toFloat(),VSize.toFloat(), paintLog )
            }

            /* Прорисовка MLEM необходима */
            if (flagMLEM) {
                YLinMLEM = (VSize - mlemBuffer[idx] * koefLinMLEM).toFloat()
                if (mlemBuffer[idx] != 0.0) {
                    YLogMLEM = (VSize - ln(mlemBuffer[idx]) * koefLogMLEM).toFloat()
                } else {
                    YLogMLEM = VSize.toFloat()
                }
                if ( ! (oldMLEMLin == VSize.toDouble() || mlemBuffer[idx] == 0.0)) {
                    /* Линейный график */
                    specCanvas.drawLine(
                        (oldXMLEM * xSize).toFloat(),   // Начальный X
                        oldMLEMLin.toFloat(),           // Начальный Y
                        (idx * xSize).toFloat(),        // Конечный X
                        YLinMLEM,                       // Конечный Y
                        paintFoneLin
                    )
                }
                /* Прорисовка логарифмического графика */
                if ( ! (oldMLEMLog == VSize.toDouble() /*&& spectrData[idx] == 0.0*/ || YLogMLEM == VSize.toFloat())) {
                    specCanvas.drawLine(
                        (oldXMLEM * xSize).toFloat(),   // Начальный X
                        oldMLEMLog.toFloat(),           // Начальный Y
                        (idx * xSize).toFloat(),        // Конечный X
                        YLogMLEM,                       // Конечный Y
                        paintFoneLog
                    )
                }
            }
            if ((Ylin.toDouble() < VSize) && (GO.specterGraphType == 0)) {
                oldYlin = Ylin.toDouble()
                oldYlog = Ylog.toDouble()
                oldX = idx.toDouble()
            }
            if (YLinMLEM.toDouble() < VSize) {
                oldMLEMLin = YLinMLEM.toDouble()
                oldMLEMLog = YLogMLEM.toDouble()
                oldXMLEM = idx.toDouble()
            }
        }
        /* Тестовая диния */
        //specCanvas.drawLine(0.0f, VSize.toFloat(), HSize.toFloat(),VSize.toFloat(), paintLin )

        imgView.setImageBitmap(specBitmap)

        //saveStat1 = txtStat1.text.toString()
        //saveStat2 = txtStat2.text.toString()
        //saveStat3 = txtStat3.text.toString()
    }

    fun clearSpecter() {
        //Log.d("BluZ-BT", "Clear specter.")
        if ((HSize > 0) and (VSize > 0)) {
            specCanvas.drawColor(Color.argb(255, 0, 0, 0))
            imgView.setImageBitmap(specBitmap)
        } else {
            Log.e("BluZ-BT", "HSize: $HSize, VSize: $VSize")
        }
    }
    fun resetSpecter() {
        //Log.d("BluZ-BT", "Clear specter.")
        for (ttt: Int in 0 until 4096) {
            tmpSpecterData[ttt] = 0.0
            spectrData[ttt] = 0.0
        }
        if ((HSize > 0) and (VSize > 0)) {
            specCanvas.drawColor(Color.argb(255, 0, 0, 0))
            imgView.setImageBitmap(specBitmap)
        } else {
            Log.e("BluZ-BT", "HSize: $HSize, VSize: $VSize")
        }
    }
}