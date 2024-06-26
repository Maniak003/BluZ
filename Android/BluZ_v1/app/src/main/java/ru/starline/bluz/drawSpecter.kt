package ru.starline.bluz

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.widget.ImageView
import android.widget.TextView

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
        HSize = imgView.width
        VSize = imgView.height
        if ((HSize == 0) or (VSize == 0)) {
            Log.e("BluZ-BT", "HSize: $HSize, VSize: $VSize")
        }
    }

    fun redrawSpecter(spType: Int, PCounter: UInt) {
        //Log.d("BluZ-BT", HSize.toString())
        //Log.d("BluZ-BT", VSize.toString())
        Log.d("BluZ-BT", "Draw specter.")
        var xSize: Float = 1f
        var paint: Paint = Paint()
        when(spType) {
            /* разрешение 1024 */
            0 -> {
                xSize = 2f
                paint.strokeWidth = 2f
            }
            /* разрешение 2048 */
            1 -> {
                xSize = 1f
                paint.strokeWidth = 1f
            }
            /* разрешение 4096 */
            2-> {
                xSize = .5f
                paint.strokeWidth = 0.5f
            }
        }
        specBitmap = Bitmap.createBitmap(HSize, VSize, Bitmap.Config.ARGB_8888)
        specCanvas = Canvas(specBitmap)
        paint.color = Color.RED

        var oldY: Float = VSize.toFloat()
        var oldX: Float = 0.0f
        var maxY: Float = 0.0f
        var koef: Float = 1.0f
        /* Поиск максимального значения для масштабирования */
        for (idx in 0..HSize) {
            if (maxY < spectrData[idx]) {
                maxY = spectrData[idx].toFloat()
            }
        }
        koef = VSize / maxY
        for (idx in 0..HSize) {
            if (! (oldY == VSize.toFloat() && spectrData[idx].toFloat() == 0f)) {
                specCanvas.drawLine(oldX * xSize, oldY, idx.toFloat() * xSize, VSize - spectrData[idx].toFloat() * koef, paint)
            }
            oldY = VSize - spectrData[idx].toFloat() * koef
            oldX = idx.toFloat()
        }
        imgView.setImageBitmap(specBitmap)

        /*
        *  Вывод статистики
        */
        txtStat1.setText(PCounter.toString())
    }

    fun clearSpecter() {
        if ((HSize > 0) and (VSize > 0)) {
            specBitmap = Bitmap.createBitmap(HSize, VSize, Bitmap.Config.ARGB_8888)
            specCanvas = Canvas(specBitmap)
            //var paint: Paint = Paint()
            //paint.color = Color.RED
            //paint.strokeWidth = 1f
            specCanvas.drawColor(Color.BLACK)
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