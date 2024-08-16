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


/**
 * Created by ed on 21,июнь,2024
 */
class drawSpecter {
    public var HSize: Int = 0
    public var VSize: Int = 0
    private lateinit var specBitmap: Bitmap
    private lateinit var specCanvas: Canvas
    public lateinit var imgView : ImageView
    public var spectrData: DoubleArray = DoubleArray(4296)
    public var tmpSpecterData: DoubleArray = DoubleArray(4296)
    public lateinit var txtStat1: TextView
    public lateinit var txtStat2: TextView
    public lateinit var txtStat3: TextView
    private var saveStat1: String = ""
    private var saveStat2: String = ""
    private var saveStat3: String = ""
    public var flagSMA: Boolean = false
    public var maxYlog: Double = 0.0
    public var ResolutionSpectr: Int = 1024;
    public var koefLog: Double = 1.0
    public var xSize: Double = 1.0

    /* Установка рабочих параметров и создание необходимых объектов */
    fun init() {
        if (GO.drawObjectInit) {
            HSize = imgView.width
            VSize = imgView.height
            if ((HSize == 0) or (VSize == 0)) {
                Log.e("BluZ-BT", "HSize: $HSize, VSize: $VSize")
            } else {
                /* Подготавливаем bitmap для рисования */
                specBitmap = Bitmap.createBitmap(HSize, VSize, Bitmap.Config.ARGB_8888)
                specCanvas = Canvas(specBitmap)
                GO.drawObjectInit =false
            }
        } else {
            imgView.setImageBitmap(specBitmap)
        }
        txtStat1.setText(saveStat1)
        txtStat2.setText(saveStat2)
        txtStat3.setText(saveStat3)
    }

    fun redrawSpecter(spType: Int) {
        Log.d("BluZ-BT", "Type: " + spType.toString() + " HSize: " + HSize.toString() + " VSize: " + VSize.toString())
        //Log.d("BluZ-BT", "Draw specter.")
        var paintLin: Paint = Paint()
        var paintLog: Paint = Paint()

        /*
         *  Подготовка массива для отрисовки данных
         *  Функии фильтрации
         */
        var tmpSM: Double
        for (ttt: Int in 0 until tmpSpecterData.size - GO.windowSMA) {
            if (flagSMA) {
                /* SMA */
                tmpSM = 0.0
                for(k in 0 until GO.windowSMA) {
                    tmpSM += spectrData[ttt + k]
                }
                tmpSpecterData[ttt] = tmpSM / GO.windowSMA
            } else {
                /* Без преобразования */
                tmpSpecterData[ttt] = spectrData[ttt]
            }
        }

        when (spType) {
            /* разрешение 1024 */
            0 -> {
                ResolutionSpectr = 1024
            }
            /* разрешение 2048 */
            1 -> {
                ResolutionSpectr = 2048
            }
            /* разрешение 4096 */
            2 -> {
                ResolutionSpectr = 4096
            }
        }
        xSize = HSize.toDouble() / ResolutionSpectr
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
        var tmpLog: Double
        var koefLin: Double
        /* Поиск максимального значения для масштабирования */
        for (idx in 0..ResolutionSpectr - 1) {
            if (maxYlin < tmpSpecterData[idx]) {
                maxYlin = tmpSpecterData[idx]
            }
            tmpLog = log(tmpSpecterData[idx])
            if (maxYlog < tmpLog) {
                maxYlog = tmpLog
            }
        }
        koefLin = VSize / maxYlin
        koefLog = VSize / maxYlog
        var Ylin: Float
        var Ylog: Float
        for (idx in 0..ResolutionSpectr - 1) {
            Ylin = (VSize - tmpSpecterData[idx] * koefLin).toFloat()
            if (tmpSpecterData[idx] != 0.0) {
                Ylog = (VSize - log(tmpSpecterData[idx]) * koefLog).toFloat()
            } else {
                Ylog = VSize.toFloat()
            }
            if (GO.specterGraphType == 0) {         // Стиль графика - линия
                /* Прорисовка линейного графика */
                if ( ! (oldYlin == VSize.toDouble() && tmpSpecterData[idx] == 0.0)) {
                    specCanvas.drawLine(
                        (oldX * xSize).toFloat(),   // Начальный X
                        oldYlin.toFloat(),          // Начальный Y
                        (idx * xSize).toFloat(),    // Конечный X
                        Ylin,                       // Конечный Y
                        paintLin
                    )
                }
                /* Прорисовка логарифмического графика */
                if ( ! (oldYlog == VSize.toDouble() /*&& spectrData[idx] == 0.0*/ && Ylog == VSize.toFloat())) {
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
            oldYlin = Ylin.toDouble()
            oldYlog = Ylog.toDouble()
            oldX = idx.toDouble()
        }
        /* Тестовая диния */
        //specCanvas.drawLine(0.0f, VSize.toFloat(), HSize.toFloat(),VSize.toFloat(), paintLin )

        imgView.setImageBitmap(specBitmap)

        /*
        *  Вывод статистики
        */
        /* Перевод в дни, часы, минуты, секунды */
        var dd: Int = GO.messTm.toInt() / 86400
        var hh: Int = (GO.messTm.toInt() - dd * 86400) /  3600
        var mm: Int = GO.messTm.toInt() / 60 % 60
        var ss: Int = GO.messTm.toInt() / 1 % 60

        var tmpStr = String.format("Time:%02d:%02d:%02d:%02d",  dd, hh, mm, ss)
        Log.d("BluZ-BT", tmpStr)
        var tmpMC: Int = GO.tempMC.toInt()
        var tmpBL = GO.battLevel

        if (GO.battLevel < 3.0f) {  // Уровень батареи низкий
            txtStat1.setText(Html.fromHtml("$tmpMC&#176C   <font color=#C80000> $tmpBL v </font>$tmpStr", HtmlCompat.FROM_HTML_MODE_LEGACY))
        } else if (GO.battLevel < 3.5f) { // Уровнь батареи ниже 50%
            txtStat1.setText(Html.fromHtml("$tmpMC&#176C   <font color=#ffff00> $tmpBL v </font>$tmpStr", HtmlCompat.FROM_HTML_MODE_LEGACY))
        } else {
            txtStat1.setText(Html.fromHtml("$tmpMC&#176C   <font color=#00ff00> $tmpBL v </font>$tmpStr", HtmlCompat.FROM_HTML_MODE_LEGACY))
        }

        /* Расчет погрешности по трем сигмам */
        var aquracy3S: Double = 300.0 / sqrt(GO.PCounter.toDouble())

        txtStat2.setText(String.format("Total:%d(%.2f%%) Avg:%.2f", GO.PCounter.toInt(), aquracy3S, GO.cps))
        var tmpPS = GO.pulsePerSec

        /* Перевод CPS в uRh */
        var doze: Float =  round(tmpPS.toFloat() * GO.propCPS2UR * 100.0f) / 100.0f
        var avgDoze : Float = (round(GO.cps * GO.propCPS2UR * 100.0f) / 100.0f).toFloat()
        txtStat3.setText("CPS:$tmpPS ($doze uRh) Avg:$avgDoze uRh")

        saveStat1 = txtStat1.text.toString()
        saveStat2 = txtStat2.text.toString()
        saveStat3 = txtStat3.text.toString()
    }

    fun clearSpecter() {
        //Log.d("BluZ-BT", "Clear specter.")
        if ((HSize > 0) and (VSize > 0)) {
            //specBitmap = Bitmap.createBitmap(HSize, VSize, Bitmap.Config.ARGB_8888)
            //specCanvas = Canvas(specBitmap)
            specCanvas.drawColor(Color.argb(255, 0, 0, 0))
            imgView.setImageBitmap(specBitmap)
        } else {
            Log.e("BluZ-BT", "HSize: $HSize, VSize: $VSize")
        }
    }
}