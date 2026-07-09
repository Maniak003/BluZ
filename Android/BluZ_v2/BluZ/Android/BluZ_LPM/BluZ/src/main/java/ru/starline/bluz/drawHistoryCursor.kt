package ru.starline.bluz

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withRotation
import kotlin.math.ln
import kotlin.math.round

/**
 * Created by ed on 07,июль,2026
 */
class drawHistoryCursor {
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

    /** Создаёт (или пересоздаёт при `GO.drawCursorObjectInit == true`) [cursorBitmap].
     *  При успешной инициализации сбрасывает [oldX] / [oldY] — иначе `hideCursor` в новой
     *  ориентации стирал бы пиксели по координатам прошлой. */
    public fun init() {
        if (!this::cursorView.isInitialized) {
            Log.e("BluZ-BT", "cursorView not init.")
            return
        }
        // Пересоздаём bitmap при первом запуске или после поворота экрана (флаг GO.drawCursorObjectInit).
        // Иначе bitmap остаётся со старыми HSize/VSize, и курсор рисуется не на той позиции,
        // потому что новый cursorView имеет другие размеры.
        if (GO.drawHistoryCursorObjectInit || !this::cursorBitmap.isInitialized) {
            HSize = cursorView.width
            VSize = cursorView.height
            if ((HSize == 0) or (VSize == 0)) {
                Log.e("BluZ-BT", "History cursor HSize: $HSize, VSize: $VSize")
            } else {
                cursorBitmap = createBitmap(HSize, VSize)
                cursorCanvas = Canvas(cursorBitmap)
                drawCursorInit = true
                GO.drawHistoryCursorObjectInit = false
                // Сбросим сохранённую позицию старого курсора, иначе hideCursor попытается
                // стереть пиксели по координатам прошлой ориентации экрана.
                oldX = 0.0f
                oldY = 0.0f
                cursorView.setImageBitmap(cursorBitmap)
            }
        }

        //cursorView.setImageBitmap(cursorBitmap)
    }

    /** Стирает старый курсор (вертикаль + круг + подписи) с canvas через
     *  `PorterDuff.Mode.CLEAR`. Использует сохранённые [oldX] / `Ylog` от предыдущего показа. */
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
            if(GO.enrgCalc.pA == 0f && GO.enrgCalc.pB == 0f && GO.enrgCalc.pC == 0f) {
                cursorCanvas.drawText(tmpEnergy.toString(),oldX + 3,Ylog + 10,  /*HSize - textVShift*/ hCursor)
            } else {
                cursorCanvas.drawText(tmpEnergy.toString() + "keV/" + tmpChann.toString(),oldX + 3,Ylog + 10,  /*HSize - textVShift*/ hCursor)
            }
            cursorCanvas.restore()
        }
    }

    /**
     * Перерисовывает курсор в новой позиции (x, y) — горизонтальная координата `x`
     * на экране, вертикальная `y` ведёт к канал-логарифм линии.
     *
     *  1. Стирает старый ([hideCursor])
     *  2. Рисует вертикаль от Y=0 до Y=[VSize]
     *  3. Вычисляет номер канала: `(x / drawSPECTER.xSize + xPosition).toInt()`
     *  4. По полиному channelToEnergy (chan: Int) энергия в кэВ (если калибровка задана)
     *  5. Рисует кружок на пересечении с логарифмическим графиком + подписи
     *  6. Ищет изотоп в справочнике через [globalObj.findIsotop] → подсказка в [globalObj.txtIsotopInfo]
     *
     * @param x Координата X в пикселях относительно [cursorView].
     * @param y Координата Y. Не используется напрямую — y определяется значением спектра.
     */
    public fun showCorsor(x: Float, y: Float) {
        if (drawCursorInit) {
            val aCursor: Paint = Paint()
            aCursor.color = GO.ColorActiveCursor
            aCursor.textSize = 20.0f;
            aCursor.isFilterBitmap = false;
            aCursor.isAntiAlias = false;
            aCursor.isDither = false;
            hideCursor()
            /* Вертикальный курсор — до низа canvas, т.е. до VSize, а не HSize */
            cursorCanvas.drawLine(x, 0.0f, x, VSize.toFloat(), aCursor);

            /* Указатель горизонтального положения */
            curChan = (x / GO.drawHISTORY.xSize + GO.xPosition).toInt()
            if (curChan > 4095) {
                curChan = 4095
            }
            if (curChan < 0) {
                curChan = 0
            }
            tmpChann = curChan
            if (GO.enrgCalc.pA == 0.0f && GO.enrgCalc.pB == 0f && GO.enrgCalc.pC == 0f) {
                tmpEnergy = curChan
            } else {
                /* Пересчет канала в энергию */
                tmpEnergy = GO.enrgCalc.channelToEnergy(curChan).toInt()
            }
            tmpCounts = GO.drawHISTORY.tmpHistoryData[curChan].toInt()
            if (GO.drawHISTORY.tmpHistoryData[curChan] != 0.0) {
                if (GO.drawHISTORY.tmpHistoryData[curChan] == 1.0) {    // Курсор по линейному графику при 0 значения логарифмического.
                    Ylog = (VSize - GO.drawHISTORY.koefLin).toFloat()
                } else {
                    Ylog = (VSize - ln(GO.drawHISTORY.tmpHistoryData[curChan]) * GO.drawHISTORY.koefLog).toFloat()
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
                if (GO.enrgCalc.pA == 0f && GO.enrgCalc.pB == 0f && GO.enrgCalc.pC == 0f) {
                    drawText(tmpEnergy.toString(), x + 3, Ylog + 10 /*HSize - textVShift*/, aCursor); // Energy
                } else {
                    drawText(tmpEnergy.toString() + "keV/" + tmpChann.toString(), x + 3, Ylog + 10 /*HSize - textVShift*/, aCursor); // Energy
                }
            }

            oldX = x
            oldY = y
            cursorView.setImageBitmap(cursorBitmap)

            /* Вывод данных из справочника изотопов */
            if(GO.enrgCalc.pA != 0f || GO.enrgCalc.pA != 0f || GO.enrgCalc.pC != 0f) {      // Имеет смысл только при наличии коэффициентов полинома
                var isotop: globalObj.IsotopsCls = GO.findIsotop(tmpEnergy)
                if (isotop.Energy == 0) {   // Изотоп в справочнике не найден
                    GO.txtHistoryIsotop?.visibility = android.view.View.INVISIBLE
                    GO.txtHistoryIsotop?.text = ""
                } else {
                    GO.txtHistoryIsotop?.visibility = android.view.View.VISIBLE
                    val low = isotop.Channel - GO.realResolution
                    val high = isotop.Channel + GO.realResolution
                    val n = GO.drawHISTORY.historyData.size
                    // Активность считается только если окно [low..high] целиком внутри спектра
                    // и в справочнике указана энергозависимая чувствительность (Activity > 0).
                    if (isotop.Activity == 0 || low < 0 || high >= n) {
                        GO.txtHistoryIsotop?.text = isotop.Name + " " + isotop.Energy.toString() + "кэВ"
                    } else {
                        /*
                        *   Расчитываем фоновую активность
                        *   (Y[isotop.Channel - GO.realResolution] + Y[isotop.Channel + GO.realResolution]) / 2 * 2 * GO.realResolution
                        */
                        var cntFon : Int = GO.realResolution * (GO.drawHISTORY.historyData[low].toInt() + GO.drawHISTORY.historyData[high].toInt())

                        /* Получим количество импульсов в диапазоне разрешения */
                        var cntPulse: Int = 0
                        for (ixCh in low .. high) {
                            cntPulse += GO.drawHISTORY.historyData[ixCh].toInt()
                        }
                        /* Расчет активности */
                        cntPulse -= cntFon
                        var activ: Float = isotop.Activity.toFloat() * cntPulse / GO.historyTime.toFloat()
                        if (activ < 0.0f) {     // Да, такое может быть из-за не правильного расчета фона
                            activ = 0.0f
                        }
                        GO.txtHistoryIsotop?.text = isotop.Name + " " + isotop.Energy.toString() + "кэВ · " + round(activ).toString() + " Бк"
                    }
                }
            } else {
                // Калибровки нет — справочник не имеет смысла, скрываем подсказку.
                GO.txtHistoryIsotop?.visibility = android.view.View.INVISIBLE
            }

        } else {
            init()
        }
    }
}