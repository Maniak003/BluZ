package ru.starline.bluz

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log
import android.widget.ImageView
import java.lang.Math.log

class drawCursor {
    public var oldX: Float = 0.0f
    public var oldY: Float = 0.0f
    public lateinit var cursorView: ImageView
    private var HSize: Int = 0
    private var VSize: Int = 0
    private lateinit var cursorBitmap: Bitmap
    private lateinit var cursorCanvas: Canvas
    private var drawCursorInit: Boolean = false
    private var Ylog: Float = 0.0f

    /* Инициализация */
    public fun init() {
        if (! drawCursorInit) {
            HSize = cursorView.width
            VSize = cursorView.height
            if ((HSize == 0) or (VSize == 0)) {
                Log.e("BluZ-BT", "HSize: $HSize, VSize: $VSize")
            } else {
                //Log.e("BluZ-BT", "HSize: $HSize, VSize: $VSize")
                /* Подготавливаем bitmap для рисования */
                cursorBitmap = Bitmap.createBitmap(HSize, VSize, Bitmap.Config.ARGB_8888)
                cursorCanvas = Canvas(cursorBitmap)
                drawCursorInit = true
            }
        } else {
            cursorView.setImageBitmap(cursorBitmap)
        }
    }

    /* Убрать курсор */
    private fun hideCursor() {
        if (drawCursorInit) {
            val hCursor: Paint = Paint()
            //hCursor.color = GO.ColorEraseCursor
            hCursor.setXfermode(PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            hCursor.textSize = 20.0f;
            hCursor.isFilterBitmap = false;
            hCursor.isAntiAlias = false;
            hCursor.isDither = false;
            cursorCanvas.drawLine(oldX, 0.0f, oldX, VSize.toFloat(), hCursor);
            //cursorCanvas.drawLine(oldX - 5, Ylog, oldX + 5, Ylog, hCursor);
            hCursor.setStyle(Paint.Style.STROKE);
            cursorCanvas.drawCircle(oldX, Ylog, 10.0f, hCursor)
        }
    }

    /* Перерисовка курсора */
    public fun showCorsor(x: Float, y: Float) {
        if (drawCursorInit) {
            val aCursor: Paint = Paint()
            aCursor.color = GO.ColorActiveCursor
            aCursor.textSize = 20.0f;
            aCursor.isFilterBitmap = false;
            aCursor.isAntiAlias = false;
            aCursor.isDither = false;
            hideCursor()
            /* Вертикальный курсор*/
            cursorCanvas.drawLine(x, 0.0f, x, HSize.toFloat(), aCursor);

            /* Горизонтальный курсор */
            var idx: Int
            idx = (x / GO.drawSPECTER.xSize).toInt()
            if (GO.drawSPECTER.tmpSpecterData[idx] != 0.0) {
                Ylog = (VSize - log(GO.drawSPECTER.tmpSpecterData[idx]) * GO.drawSPECTER.koefLog).toFloat()
            } else {
                Ylog = VSize.toFloat()
            }
            //cursorCanvas.drawLine(x - 5, Ylog, x + 5, Ylog, aCursor);
            aCursor.setStyle(Paint.Style.STROKE);
            cursorCanvas.drawCircle(x, Ylog, 10.0f, aCursor)

            oldX = x
            oldY = y
            cursorView.setImageBitmap(cursorBitmap)
        } else {
            init()
        }
    }
}