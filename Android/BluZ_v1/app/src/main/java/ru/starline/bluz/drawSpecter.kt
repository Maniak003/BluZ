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
    private var HSize: Int = 0
    private var VSize: Int = 0
    private lateinit var specBitmap: Bitmap
    private lateinit var specCanvas: Canvas
    lateinit var imgView : ImageView

    fun init() {
        HSize = imgView.width
        VSize = imgView.height
    }

    fun testLine() {
        Log.d("BluZ-BT", HSize.toString())
        Log.d("BluZ-BT", VSize.toString())
        specBitmap = Bitmap.createBitmap(HSize, VSize, Bitmap.Config.ARGB_8888)
        specCanvas = Canvas(specBitmap)
        var paint: Paint = Paint()
        paint.color = Color.RED
        paint.strokeWidth = 1f
        specCanvas.drawLine(0.0f, 0.0f, HSize.toFloat(), VSize.toFloat(), paint)
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