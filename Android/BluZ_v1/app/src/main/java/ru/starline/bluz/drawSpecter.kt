package ru.starline.bluz

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import ru.starline.bluz.MainActivity
import java.lang.Math.log


/**
 * Created by ed on 21,июнь,2024
 */
class drawSpecter {
    public var HSize: Int = 0
    public var VSize: Int = 0
    private lateinit var specBitmap: Bitmap
    private lateinit var specCanvas: Canvas
    private lateinit var imgView : ImageView
    public var spectrData: DoubleArray = DoubleArray(4096)
    public lateinit var txtStat1: TextView
    public lateinit var txtStat2: TextView

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
        }
    }

    fun redrawSpecter(spType: Int, PCounter: UInt) {
        //Log.d("BluZ-BT", HSize.toString())
        //Log.d("BluZ-BT", VSize.toString())
        Log.d("BluZ-BT", "Draw specter.")
        var xSize: Float = 1f
        var paintLin: Paint = Paint()
        var paintLog: Paint = Paint()
        when(spType) {
            /* разрешение 1024 */
            0 -> {
                xSize = 2f
                paintLin.strokeWidth = 2f
            }
            /* разрешение 2048 */
            1 -> {
                xSize = 1f
                paintLin.strokeWidth = 1f
            }
            /* разрешение 4096 */
            2-> {
                xSize = .5f
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
        var koefLin: Double = 1.0
        var koefLog: Double = 1.0
        /* Поиск максимального значения для масштабирования */
        for (idx in 0..HSize) {
            if (maxYlin < spectrData[idx]) {
                maxYlin = spectrData[idx]
            }
            if (maxYlog < spectrData[idx]) {
                maxYlog = log(spectrData[idx])
            }
        }
        koefLin = VSize / maxYlin
        koefLog = VSize / maxYlog
        for (idx in 0..HSize) {
            if (! (oldYlin == VSize.toDouble() && spectrData[idx] == 0.0)) {
                specCanvas.drawLine((oldX * xSize).toFloat(), oldYlin.toFloat(), idx.toFloat() * xSize, (VSize - spectrData[idx] * koefLin).toFloat(), paintLin)
                specCanvas.drawLine((oldX * xSize).toFloat(), oldYlog.toFloat(), idx.toFloat() * xSize, (VSize - log(spectrData[idx]) * koefLog).toFloat(), paintLog)
            }
            oldYlin = VSize - spectrData[idx].toFloat() * koefLin
            oldYlog = VSize - log(spectrData[idx]) * koefLog
            oldX = idx.toDouble()
        }
        imgView.setImageBitmap(specBitmap)

        /*
        *  Вывод статистики
        */
        txtStat1.setText(PCounter.toString())
    }

    fun clearSpecter() {
        if ((HSize > 0) and (VSize > 0)) {
            //specBitmap = Bitmap.createBitmap(HSize, VSize, Bitmap.Config.ARGB_8888)
            //specCanvas = Canvas(specBitmap)
            specCanvas.drawColor(Color.argb(255,0, 0, 0))
            imgView.setImageBitmap(specBitmap)
        } else {
            Log.e("BluZ-BT", "HSize: $HSize, VSize: $VSize")
        }
    }

    constructor(view: ImageView, Stat1 : TextView, Stat2: TextView ) {
        imgView = view
        txtStat1 = Stat1
        txtStat2 = Stat2
    }
}