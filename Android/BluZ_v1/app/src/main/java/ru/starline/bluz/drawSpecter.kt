package ru.starline.bluz

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.widget.ImageView

/**
 * Created by ed on 21,июнь,2024
 */
class drawSpecter {
    public var HSize: Int = 0
    public var VSize: Int = 0
    private lateinit var specBitmap: Bitmap
    private lateinit var specCanvas: Canvas
    lateinit var imgView : ImageView
    public var spectrData: DoubleArray = DoubleArray(4096)

    fun init() {
        HSize = imgView.width
        VSize = imgView.height
    }

    fun testLine() {
        //Log.d("BluZ-BT", HSize.toString())
        //Log.d("BluZ-BT", VSize.toString())
        specBitmap = Bitmap.createBitmap(HSize, VSize, Bitmap.Config.ARGB_8888)
        specCanvas = Canvas(specBitmap)
        var paint: Paint = Paint()
        paint.color = Color.RED
        paint.strokeWidth = 1f
        var oldY: Float = 0.0f
        var oldX: Float = 0.0f
        var maxY: Float = 0.0f
        var koef: Float = 1.0f
        for (idx in 0..HSize) {
            if (maxY < spectrData[idx]) {
                maxY = spectrData[idx].toFloat()
            }
        }
        koef = VSize / maxY
        for (idx in 0..HSize) {
            specCanvas.drawLine(oldX, oldY, idx.toFloat(), VSize - spectrData[idx].toFloat() * koef, paint)
            oldY = VSize - spectrData[idx].toFloat() * koef
            oldX = idx.toFloat()
        }
        imgView.setImageBitmap(specBitmap)
    }

    fun clearSpecter() {
        specBitmap = Bitmap.createBitmap(HSize, VSize, Bitmap.Config.ARGB_8888)
        specCanvas = Canvas(specBitmap)
        var paint: Paint = Paint()
        paint.color = Color.RED
        paint.strokeWidth = 1f
        specCanvas.drawColor(Color.BLACK)
        imgView.setImageBitmap(specBitmap)
    }

    constructor(view: ImageView) {
        imgView = view
    }
}