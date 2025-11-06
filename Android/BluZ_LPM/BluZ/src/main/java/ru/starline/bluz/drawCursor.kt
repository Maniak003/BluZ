package ru.starline.bluz

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log
import android.widget.ImageView
import java.lang.Math.log
import kotlin.math.ln
import kotlin.math.round
import androidx.core.graphics.withRotation

class drawCursor {
    public var oldX: Float = 0.0f
    public var oldY: Float = 0.0f
    public var curChan: Int = 0
    public lateinit var cursorView: ImageView
    private var HSize: Int = 0
    private var VSize: Int = 0
    private lateinit var cursorBitmap: Bitmap
    private lateinit var cursorCanvas: Canvas
    public var drawCursorInit: Boolean = false
    private var Ylog: Float = 0.0f
    private var tmpCounts: Int = 0
    private var tmpEnergy: Int = 0
    private var tmpChann: Int = 0
    private var cfA : Float = 0.0f
    private var cfB : Float = 0.0f
    private var cfC : Float = 0.0f

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
            hCursor.style = Paint.Style.STROKE;
            cursorCanvas.drawCircle(oldX, Ylog, 10.0f, hCursor)
            hCursor.style = Paint.Style.FILL;
            cursorCanvas.drawText(tmpCounts.toString(), oldX + 10, Ylog + 4, hCursor) //Erase counts text
            cursorCanvas.save()
            cursorCanvas.rotate(90f, oldX + 3, Ylog + 10 /*HSize - textVShift*/)
            if(cfA == 0.0f) {
                cursorCanvas.drawText(tmpEnergy.toString(),oldX + 3,Ylog + 10,  /*HSize - textVShift*/ hCursor)
            } else {
                cursorCanvas.drawText(tmpEnergy.toString() + "keV/" + tmpChann.toString(),oldX + 3,Ylog + 10,  /*HSize - textVShift*/ hCursor)
            }
            cursorCanvas.restore()
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

            /* Указатель горизонтального положения */
            curChan = (x / GO.drawSPECTER.xSize).toInt()
            if (curChan > 4095) {
                curChan = 4095
            }
            if (curChan < 0) {
                curChan = 0
            }
            when (GO.specterType) {
                0 -> {  // 1024
                    cfA = GO.propCoef1024A
                    cfB = GO.propCoef1024B
                    cfC = GO.propCoef1024C
                }
                1 -> {  // 2048
                    cfA = GO.propCoef2048A
                    cfB = GO.propCoef2048B
                    cfC = GO.propCoef2048C
                }
                2 -> {  // 4096
                    cfA = GO.propCoef4096A
                    cfB = GO.propCoef4096B
                    cfC = GO.propCoef4096C
                }
            }
            tmpChann = curChan
            if (cfA == 0.0f) {
                tmpEnergy = curChan
            } else {
                /* Пересчет канала в энергию */
                tmpEnergy = (cfA * curChan * curChan + cfB * curChan + cfC).toInt()
            }
            tmpCounts = GO.drawSPECTER.tmpSpecterData[curChan].toInt()
            if (GO.drawSPECTER.tmpSpecterData[curChan] != 0.0) {
                if (GO.drawSPECTER.tmpSpecterData[curChan] == 1.0) {    // Курсор по линейному графику при 0 значения логарифмического.
                    Ylog = (VSize - GO.drawSPECTER.koefLin).toFloat()
                } else {
                    Ylog = (VSize - ln(GO.drawSPECTER.tmpSpecterData[curChan]) * GO.drawSPECTER.koefLog).toFloat()
                }
            } else {
                Ylog = VSize.toFloat()
            }
            //cursorCanvas.drawLine(x - 5, Ylog, x + 5, Ylog, aCursor);
            aCursor.style = Paint.Style.STROKE;
            cursorCanvas.drawCircle(x, Ylog, 10.0f, aCursor)
            aCursor.style = Paint.Style.FILL;
            cursorCanvas.drawText(tmpCounts.toString(), x + 10, Ylog + 4, aCursor) // Counts
            cursorCanvas.withRotation(90f, x + 3, Ylog + 10 /*HSize - textVShift*/) {
                if (cfA == 0.0f) {
                    drawText(tmpEnergy.toString(), x + 3, Ylog + 10 /*HSize - textVShift*/, aCursor); // Energy
                } else {
                    drawText(tmpEnergy.toString() + "keV/" + tmpChann.toString(), x + 3, Ylog + 10 /*HSize - textVShift*/, aCursor); // Energy
                }
            };

            oldX = x
            oldY = y
            cursorView.setImageBitmap(cursorBitmap)

            /* Вывод данных из справочника изотопов */
            if(cfA != 0.0f) {      // Имеет смысл только при наличии коэффициентов полинома
                var isotop: globalObj.IsotopsCls = GO.findIsotop(tmpEnergy)
                if (isotop.Energy == 0) {   // Изотоп в справочнике не найден
                    GO.txtIsotopInfo.text = "";
                } else {
                    if (isotop.Activity == 0) {
                        GO.txtIsotopInfo.text = isotop.Name + " " + isotop.Energy.toString() + "kEv"
                    } else {
                        /*
                        *   Расчитываем фоновую активность
                        *   (Y[isotop.Channel - GO.realResolution] + Y[isotop.Channel + GO.realResolution]) / 2 * 2 * GO.realResolution
                        */
                        var cntFon : Int = GO.realResolution * (GO.drawSPECTER.spectrData[isotop.Channel - GO.realResolution].toInt() + GO.drawSPECTER.spectrData[isotop.Channel + GO.realResolution].toInt())

                        /* Получим количество импульсов в диапазоне разрешения */
                        var cntPulse: Int = 0
                        for (ixCh in isotop.Channel - GO.realResolution .. isotop.Channel + GO.realResolution) {
                            cntPulse += GO.drawSPECTER.spectrData[ixCh].toInt()
                        }
                        /* Расчет активности */
                        cntPulse -= cntFon
                        var activ: Float = isotop.Activity.toFloat() * cntPulse / GO.spectrometerTime.toFloat()
                        if (activ < 0.0f) {     // Да, такое может быть из-за не правильного расчета фона
                            activ = 0.0f
                        }
                        GO.txtIsotopInfo.text = isotop.Name + " " + isotop.Energy.toString() + "kEv Act:" + round(activ).toString() + " Bq"
                    }
                }
            }

        } else {
            init()
        }
    }
}