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
    public var spectrData: DoubleArray = DoubleArray(4096)
    public lateinit var txtStat1: TextView
    public lateinit var txtStat2: TextView
    public lateinit var txtStat3: TextView
    private var saveStat1: String = ""
    private var saveStat2: String = ""
    private var saveStat3: String = ""

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

    fun redrawSpecter(spType: Int, PCounter: UInt, cps: Float, messTm:ULong, battLevel: UByte, tempMC: Float) {
        //Log.d("BluZ-BT", "HSize: " + HSize.toString() + " VSize: " + VSize.toString())
        //Log.d("BluZ-BT", "Draw specter.")
        var xSize: Double = 1.0
        var paintLin: Paint = Paint()
        var paintLog: Paint = Paint()
        when (spType) {
            /* разрешение 1024 */
            0 -> {
                xSize = 2.0
                paintLin.strokeWidth = 2f
            }
            /* разрешение 2048 */
            1 -> {
                xSize = 1.0
                paintLin.strokeWidth = 1f
            }
            /* разрешение 4096 */
            2 -> {
                xSize = 0.5
                paintLin.strokeWidth = 0.5f
            }
        }

        paintLin.color = GO.ColorLin        // Цвет для отображения линейного спектра
        paintLog.color = GO.ColorLog        // Цвет для отображения логарифмического спектра
        var oldYlin: Double = VSize.toDouble()
        var oldYlog: Double = VSize.toDouble()
        var oldX: Double = 0.0
        var maxYlin: Double = 0.0
        var maxYlog: Double = 0.0
        var tmpLog: Double
        var koefLin: Double
        var koefLog: Double
        /* Поиск максимального значения для масштабирования */
        for (idx in 0..HSize) {
            if (maxYlin < spectrData[idx]) {
                maxYlin = spectrData[idx]
            }
            tmpLog = log(spectrData[idx])
            if (maxYlog < tmpLog) {
                maxYlog = tmpLog
            }
        }
        koefLin = VSize / maxYlin
        koefLog = VSize / maxYlog
        var Ylin: Float
        var Ylog: Float
        for (idx in 0..HSize) {
            Ylin = (VSize - spectrData[idx] * koefLin).toFloat()
            if (spectrData[idx] != 0.0) {
                Ylog = (VSize - log(spectrData[idx]) * koefLog).toFloat()
            } else {
                Ylog = VSize.toFloat()
            }
            /* Прорисовка линейного графика */
            if ( ! (oldYlin == VSize.toDouble() && spectrData[idx] == 0.0)) {
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
            oldYlin = Ylin.toDouble()
            oldYlog = Ylog.toDouble()
            oldX = idx.toDouble()
        }
        imgView.setImageBitmap(specBitmap)

        /*
        *  Вывод статистики
        */
        /*
        * "%.0f&#176C <font color=#ffff00> %.0f%%</font> total: %.0f cps: %.0f"
        */
        //var loc: Locale = Locale.US
        if (battLevel < 11u) {
            txtStat1.setText(Html.fromHtml("$tempMC&#176C   <font color=#C80000> $battLevel%</font>", HtmlCompat.FROM_HTML_MODE_LEGACY))
        } else if (battLevel < 50u) {
            txtStat1.setText(Html.fromHtml("$tempMC&#176C   <font color=#ffff00> $battLevel%</font>", HtmlCompat.FROM_HTML_MODE_LEGACY))
        } else {
            txtStat1.setText(Html.fromHtml("$tempMC&#176C   <font color=#00ff00> $battLevel%</font>", HtmlCompat.FROM_HTML_MODE_LEGACY))
        }

        txtStat2.setText("Total: $PCounter")
        txtStat3.setText("CPS: $cps")
        //txtStat1.setText(Html.fromHtml(stat, HtmlCompat.FROM_HTML_MODE_LEGACY))
        //stat = String.format(loc, "time: %.0f avg: %.2f (100", 0.0f, 0.0f) + "%)"
        //txtStat2.setText(stat)

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